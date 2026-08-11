Here is the comprehensive System Design Document for the Reejuven8 Core Platform and the NineMo vertical, detailing the microservices architecture, polyglot database schema, hybrid event-driven topology, and low-level security compliance for the Ayushman Bharat Digital Mission (ABDM).

### **1. Microservices Architecture (The Distributed Engine)**

The backend relies on bounded contexts mapped to specific microservices to ensure the platform scales horizontally while maintaining a clean separation between the domain-agnostic core (Reejuven8) and specialized clinical logic (NineMo).

* **`identity-abha-service` (Core Platform)**
* 
**Responsibilities:** Manages ABHA creation, identity linking, and consent flows. It acts as the gateway for asynchronous callbacks from the ABDM network.


* 
**Tech Stack:** Spring Boot, Redis (for temporary state caching during callbacks), and PostgreSQL.


* 
**Internal Logic:** Implements `CallbackController` endpoints to receive data from the ABDM Gateway asynchronously and handles the necessary RSA and AES encryption/decryption.




* **`health-data-service` (Core Platform)**
* 
**Responsibilities:** Serves as the primary gateway to the FHIR data lake. Handles saving, retrieving, and securing all medical records and unstructured files.


* 
**Tech Stack:** Spring Boot, MongoDB (FHIR JSONs), and AWS S3 (physical files).




* **`ai-parsing-service` (Core Platform)**
* 
**Responsibilities:** Translates unstructured Indian medical data into structured insights.


* 
**Tech Stack:** Python (FastAPI/Django) or Spring Boot integrated with the AWS Textract API.


* 
**Internal Logic:** Executes CPU-intensive Optical Character Recognition (OCR) and utilizes custom Small Language Models (SLMs) and Medical Named Entity Recognition (NER) to extract specific entities like vitals or medications from unstructured images/PDFs.




* **`notification-service` (Core Platform)**
* 
**Responsibilities:** An omnichannel orchestrator that dispatches automated alerts for appointment reminders, report readiness, and clinical risks.


* 
**Tech Stack:** Spring Boot asynchronous workers. Integrates with external APIs like Twilio or Gupshup for SMS and WhatsApp delivery.




* **`ninemo-clinical-service` (Vertical Module)**
* 
**Responsibilities:** Houses the specialized maternity logic, gestational timeline engine, medical auto-scheduling, and WHO pediatric growth charts.


* 
**Tech Stack:** Java, Spring Boot, and a Rule Engine (like Drools or pure Java logic).


* 
**Internal Logic:** Processes input from the `symptom_logs` and parsed vitals, cross-referencing them against the user's specific gestational week to trigger red-flag alerts or context-aware insights.




* **`ninemo-community-service` (Vertical Module)**
* 
**Responsibilities:** Manages the "Due Date Clubs" and real-time chat functionalities.


* 
**Tech Stack:** Spring Boot utilizing WebSockets and the STOMP protocol.





---

### **2. Polyglot Database Schema**

Medical data pairs strict, legally binding user identities with flexible, deeply nested clinical observations. This is solved using a Polyglot Persistence strategy.

#### **A. Relational Anchor (PostgreSQL)**

Handles strict ACID-compliant transactions for identities, relationships, and appointments.

| Table Name | Fields & Constraints | Purpose |
| --- | --- | --- |
| **`users`** | <br>`id` (UUID, PK), `abha_address` (Varchar, Unique), `phone_number` (Varchar, Unique), `email_id` (Varchar, Unique), `role` (Enum: PATIENT, DOCTOR, ADMIN), `profile_picture_url` (Varchar), `is_active` (Boolean), `created_at`, `updated_at` .

 | The global identifier and core authentication anchor.

 |
| **`patient_profiles`** | <br>`id` (UUID, PK), `user_id` (UUID, FK), `date_of_birth` (Date), `biological_sex` (Enum: MALE, FEMALE, OTHER), `emergency_contact_name` (Varchar), `emergency_contact_number` (Varchar) .

 | Strict 1:1 extension of the `users` table holding general demographics.

 |
| **`doctor_profiles`** | <br>`id` (UUID, PK), `user_id` (UUID, FK), `medical_license_number` (Varchar, Unique), `specialization` (Varchar), `qualifications` (Varchar), `years_of_experience` (Integer), `consultation_fee` (Decimal), `bio` (Text), `digital_signature_url` (Varchar), `is_accepting_patients` (Boolean) .

 | Strict 1:1 extension tracking professional credentials and compliance markers.

 |
| **`addresses`** | <br>`id` (UUID, PK), `user_id` (UUID, FK), `address_type` (Enum: HOME, CLINIC, BILLING), `address_line_1` (Varchar), `address_line_2` (Varchar), `city` (Varchar), `state` (Varchar), `pincode` (Varchar), `country` (Varchar) .

 | 1:N relationship extracting location data from the core users table.

 |
| **`pregnancy_profiles`** | <br>`id` (UUID, PK), `user_id` (UUID, FK), `lmp_date` (Date), `ultrasound_date` (Date), `ivf_transfer_date` (Date), `edd_date` (Date), `height_cm` (Decimal), `pre_pregnancy_weight_kg` (Decimal), `baseline_bmi` (Decimal), `blood_group` (Varchar), `high_risk_flags` (JSONB), `is_active` (Boolean), `delivery_date` (Date) .

 | 1:N extension storing baseline metrics for the NineMo timeline engine.

 |
| **`appointments`** | <br>`id` (UUID, PK), `patient_id` (UUID, FK), `doctor_id` (UUID, FK), `scheduled_time` (Timestamp), `status` (Enum: BOOKED, COMPLETED, CANCELLED).

 | Handles the transactional booking engine.

 |
| **`user_consents`** | <br>`id` (UUID, PK), `patient_id` (UUID, FK), `doctor_id` (UUID, FK), `consent_status` (Enum: GRANTED, REVOKED, EXPIRED), `granted_at` (Timestamp), `expires_at` (Timestamp) .

 | 1:N relationship tracking the Unified Consent Management system.

 |

#### **B. The Flexible Data Lake (MongoDB)**

Acts as the primary storage for unstructured medical records and complex FHIR JSON bundles.

| Collection Name | Fields & Structure | Purpose |
| --- | --- | --- |
| **`fhir_resources`** | <br>`_id` (ObjectId), `patient_id` (UUID), `resource_type` (String), `effective_datetime` (Timestamp), `category` (String), `code` (Object), `value_quantity` (Object), `source_file_s3_url` (String).

 | Stores medical entities natively as FHIR resources rather than rigid SQL tables.

 |
| **`ninemo_timeline_feed`** | <br>`_id` (ObjectId), `pregnancy_profile_id` (UUID), `gestational_week` (Integer), `baby_size_comparison` (String), `maternal_symptoms_expected` (Array), `scheduled_milestones` (Array of Objects), `pinned_reports` (Array of ObjectIds).

 | Powers the dynamic frontend feed to render weekly content without complex joins.

 |
| **`symptom_logs`** | <br>`_id` (ObjectId), `patient_id` (UUID), `gestational_week_at_log` (Integer), `symptoms` (Array of Strings), `severity_flag` (String), `logged_at` (Timestamp).

 | Feeds into the Java Rule Engine for red-flag triage logic.

 |

---

### **3. Hybrid Event-Driven Architecture (EDA)**

To handle the unpredictable nature of asynchronous ABDM callbacks and CPU-intensive OCR tasks, the ecosystem utilizes a hybrid messaging approach: Apache Kafka for immutable data streams (auditability/high throughput) and RabbitMQ for worker queues (routing/retries) .

* **Topic:** `abdm.consent.granted` **(Kafka)**
* 
**Flow:** `identity-abha-service` -> `health-data-service`.


* **Design Rationale:** Consent is a legal state change. Kafka's immutable log ensures a permanent, time-stamped audit trail crucial for compliance.




* **Topic:** `abdm.data.received` **(Kafka)**
* 
**Flow:** `identity-abha-service` -> `health-data-service`.


* 
**Design Rationale:** Kafka handles the high throughput required for massive, raw FHIR payloads returning from the government gateway.




* **Queue:** `document.unstructured.uploaded` **(RabbitMQ)**
* 
**Flow:** `health-data-service` -> `ai-parsing-service`.


* 
**Design Rationale:** Implements the "Competing Consumers" pattern, distributing heavy OCR processing tasks safely across multiple AI worker instances.




* **Topic:** `document.data.parsed` **(Kafka)**
* 
**Flow:** `ai-parsing-service` -> `ninemo-clinical-service`.


* 
**Design Rationale:** Once data is parsed, multiple downstream consumers (NineMo, data lake, analytics) require it simultaneously via a publish/subscribe model.




* **Queue:** `clinical.risk.detected` **(RabbitMQ)**
* 
**Flow:** `ninemo-clinical-service` -> `notification-service`.


* 
**Design Rationale:** Utilizes Dead Letter Exchanges (DLX) for automatic retries and exponential backoffs if an external API (like WhatsApp) fails.




* **Queue:** `patient.milestone.due` **(RabbitMQ)**
* 
**Flow:** `ninemo-clinical-service` -> `notification-service`.


* 
**Design Rationale:** Uses delayed messaging to schedule reminders at a specific future time.





---

### **4. Security, Encryption & ABDM Compliance**

ABDM mandates rigorous data protection protocols for processing and transmitting patient identity and medical records.

1. **Authentication & API Gateway:**
* 
**ABDM Layer:** The `identity-abha-service` must authenticate with the ABDM Gateway using Sandbox or Production credentials (`clientId` and `clientSecret`) to generate an `accessToken`. This is passed as a Bearer token in the headers of all external requests.


* 
**Internal Layer:** The suite enforces strict Role-Based Access Control (RBAC) via Spring Security and JWTs, dynamically checking the `role` enum (PATIENT, DOCTOR, ADMIN) established in PostgreSQL.




2. **Encryption of Sensitive Payloads (RSA):**
* All sensitive parameters passed to the ABDM APIs (e.g., Aadhaar Number, OTPs) must be RSA-encrypted using the ABDM Public Key before transit.


* The required cipher types include `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` or `PKCS1Padding`, dependent on the specific endpoint.




3. **Data Decryption & Interoperability (Curve25519/FHIR):**
* Because the government does not send Aadhaar data or health payloads as plain text, the system uses Diffie-Hellman Key Exchange and Curve25519 encryption logic to securely decrypt the incoming health data payloads.


* The `nha-abdm-wrapper` open-source library is leveraged to standardize the decryption service and callback logic.


* All parsed and verified medical data is strictly transmitted and persisted in the FHIR R4 standard to guarantee network compatibility.