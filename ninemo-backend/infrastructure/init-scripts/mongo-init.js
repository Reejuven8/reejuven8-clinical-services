// Switch to the reejuven8 database. Init scripts already run authenticated as the
// MONGO_INITDB_ROOT user (created in the admin db), so no db.auth() here — calling it
// against the reejuven8 db throws UserNotFound and aborts init (container exits 1).
db = db.getSiblingDB('reejuven8');

// ─────────────────────────────────────────────
// Create collections
// ─────────────────────────────────────────────

db.createCollection('fhir_resources');
db.createCollection('ninemo_timeline_feed');
db.createCollection('symptom_logs');
db.createCollection('vitals_logs');
db.createCollection('kick_counter_sessions');
db.createCollection('contraction_sessions');
db.createCollection('growth_measurements');
db.createCollection('developmental_milestones');
db.createCollection('due_date_clubs');
db.createCollection('chat_messages');
db.createCollection('content_articles');

// ─────────────────────────────────────────────
// Indexes
// ─────────────────────────────────────────────

// fhir_resources
db.fhir_resources.createIndex({ patient_id: 1, resource_type: 1, effective_datetime: -1 });
db.fhir_resources.createIndex({ patient_id: 1, "code.coding.code": 1 });
db.fhir_resources.createIndex({ source: 1 });
db.fhir_resources.createIndex({ tags: 1 });

// ninemo_timeline_feed
db.ninemo_timeline_feed.createIndex({ pregnancy_profile_id: 1, gestational_week: 1 }, { unique: true });

// symptom_logs
db.symptom_logs.createIndex({ patient_id: 1, logged_at: -1 });
db.symptom_logs.createIndex({ pregnancy_profile_id: 1, gestational_week_at_log: 1 });
db.symptom_logs.createIndex({ severity_flag: 1, logged_at: -1 });

// vitals_logs
db.vitals_logs.createIndex({ patient_id: 1, vital_type: 1, logged_at: -1 });
db.vitals_logs.createIndex({ pregnancy_profile_id: 1, gestational_week: 1 });

// kick_counter_sessions
db.kick_counter_sessions.createIndex({ patient_id: 1, session_start: -1 });
db.kick_counter_sessions.createIndex({ pregnancy_profile_id: 1, gestational_week: 1 });

// contraction_sessions
db.contraction_sessions.createIndex({ patient_id: 1, session_start: -1 });

// growth_measurements
db.growth_measurements.createIndex({ child_id: 1, measurement_date: -1 });
db.growth_measurements.createIndex({ child_id: 1, age_in_months: 1 });

// developmental_milestones
db.developmental_milestones.createIndex({ child_id: 1, month: 1 });

// due_date_clubs
db.due_date_clubs.createIndex({ due_date_month: 1 }, { unique: true });
db.due_date_clubs.createIndex({ "members.user_id": 1 });

// chat_messages
db.chat_messages.createIndex({ club_id: 1, channel_id: 1, sent_at: -1 });
db.chat_messages.createIndex({ sender_id: 1 });

// content_articles
db.content_articles.createIndex({ target_gestational_weeks: 1, is_published: 1 });
db.content_articles.createIndex({ target_postnatal_months: 1, is_published: 1 });
db.content_articles.createIndex({ tags: 1 });
db.content_articles.createIndex({ slug: 1 }, { unique: true });

print('MongoDB initialization complete: 11 collections created with all indexes.');
