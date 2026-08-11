## Goal Description
Pivot the architecture to establish **Reejuven8** as a comprehensive, "ERP-style" Electronic Health Record (EHR) and OPD management suite. The platform will primarily empower doctors with frictionless clinical workflows, while providing patients a seamless, transparent view of their treatments and prescriptions. 

The architecture supports a many-to-many relationship between doctors and patients. **NineMo** will be maintained as a specialized clinical tangent (maternity), built on top of the generalized OPD foundation.

## The Vision (Updated)
1. **ERP for Doctors**: A centralized hub to manage patients, view longitudinal health records, and streamline OPD workflows.
2. **Seamless Patient Experience**: Patients can effortlessly access their active treatments, e-prescriptions, and follow-up schedules.
3. **Many-to-Many Mapping**: A robust relational model allowing a patient to consult multiple doctors, and a doctor to manage thousands of patients, with data securely siloed and shared via consent.
4. **Patient Communities (TBD)**: Forums for patients to discuss conditions and seek peer support.
5. **NineMo (Maternity Tangent)**: A specialized clinical pathway specifically for pregnant patients, leveraging the core OPD infrastructure.
6. **AI-Assisted SOAP Notes**: Leveraging the `ai-parsing-service` to listen to consultations and automatically generate structured Subjective, Objective, Assessment, and Plan (SOAP) notes, drastically reducing doctor burnout.
7. **Universal Interoperability (ABHA)**: Doctors can pull a new walk-in patient's historical records from other hospitals instantly (with OTP consent), eliminating the "blind spot" of first-time visits.
8. **Smart Safety Checks**: The system automatically cross-references generated e-prescriptions against the patient's known allergies and existing medications to prevent adverse drug events.

## Proposed Changes

### [MODIFY] `ninemo-clinical-service` -> `reejuven8-clinical-service`
*   **Goal**: This becomes the beating heart of the OPD platform.
*   **New Logic**: Handles generic appointments, structured clinical notes (SOAP), e-prescription generation, and diagnosis coding (ICD-10/SNOMED). It will simply append Google Meet URLs to virtual appointments rather than reinventing WebRTC.

### [NEW] `ninemo-maternity-service`
*   **Goal**: Isolate the NineMo specific timeline and symptom triage logic so it does not bloat the general OPD service. It acts as an extension/plugin to the core clinical service.

### Database Schema Updates (PostgreSQL)

#### [MODIFY] Table: `appointments`
*   Add `meeting_url` (Varchar) to store Google Meet links for teleconsultations.
*   Add `appointment_type` (Enum: IN_PERSON, VIRTUAL).

#### [NEW] Table: `clinical_encounters` (The "Visit" Record)
*   Links a `patient_id` and `doctor_id` for a specific visit. Stores the chief complaint, vitals, and links to the generated e-prescription. This enforces the many-to-many relationship securely.

#### [NEW] Table: `doctor_patient_relationships`
*   Maintains the active roster of patients under a specific doctor's care for quick access in their "ERP" dashboard.

## Verification Plan
*   **Data Modeling Check**: Validate the `clinical_encounters` table can accurately represent a patient seeing a General Physician for a fever on Monday, and an OBGYN for NineMo tracking on Tuesday, without data bleeding or permission errors.
