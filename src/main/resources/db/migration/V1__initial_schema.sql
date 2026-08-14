CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL,
                       email VARCHAR(254) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       email_verified_at TIMESTAMPTZ,
                       mfa_enabled BOOLEAN NOT NULL,
                       mfa_secret VARCHAR(255),
                       CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE profiles (
                          id UUID PRIMARY KEY,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          user_id UUID NOT NULL,
                          first_name VARCHAR(255) NOT NULL,
                          last_name VARCHAR(255) NOT NULL,
                          date_of_birth DATE NOT NULL,
                          gender VARCHAR(255),
                          address_line_1 VARCHAR(150),
                          address_line_2 VARCHAR(150),
                          town_city VARCHAR(100),
                          county VARCHAR(100),
                          postcode VARCHAR(20),
                          country VARCHAR(100),
                          CONSTRAINT uk_profile_user UNIQUE (user_id),
                          CONSTRAINT fk_profile_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users (id)
);

CREATE TABLE patient_records (
                                 id UUID PRIMARY KEY,
                                 created_at TIMESTAMPTZ NOT NULL,
                                 updated_at TIMESTAMPTZ NOT NULL,
                                 profile_id UUID NOT NULL,
                                 nhs_number VARCHAR(10),
                                 chi_number VARCHAR(10),
                                 hc_number VARCHAR(10),
                                 height NUMERIC(6, 2),
                                 height_unit VARCHAR(255),
                                 weight NUMERIC(6, 2),
                                 weight_unit VARCHAR(255),
                                 blood_type VARCHAR(255),
                                 CONSTRAINT uk_patient_record_profile UNIQUE (profile_id),
                                 CONSTRAINT fk_patient_record_profile
                                     FOREIGN KEY (profile_id)
                                         REFERENCES profiles (id)
);

CREATE TABLE patient_carer_access (
                                      id UUID PRIMARY KEY,
                                      created_at TIMESTAMPTZ NOT NULL,
                                      updated_at TIMESTAMPTZ NOT NULL,
                                      patient_record_id UUID NOT NULL,
                                      carer_user_id UUID NOT NULL,
                                      status VARCHAR(20) NOT NULL,
                                      invited_at TIMESTAMPTZ NOT NULL,
                                      status_changed_at TIMESTAMPTZ NOT NULL,
                                      CONSTRAINT uk_patient_carer_access
                                          UNIQUE (patient_record_id, carer_user_id),
                                      CONSTRAINT fk_patient_carer_access_patient
                                          FOREIGN KEY (patient_record_id)
                                              REFERENCES patient_records (id),
                                      CONSTRAINT fk_patient_carer_access_carer
                                          FOREIGN KEY (carer_user_id)
                                              REFERENCES users (id)
);

CREATE TABLE patient_carer_access_permissions (
                                                  patient_carer_access_id UUID NOT NULL,
                                                  permission VARCHAR(100) NOT NULL,
                                                  CONSTRAINT uk_patient_carer_access_permission
                                                      UNIQUE (
                                                              patient_carer_access_id,
                                                              permission
                                                          ),
                                                  CONSTRAINT fk_patient_carer_access_permissions_access
                                                      FOREIGN KEY (patient_carer_access_id)
                                                          REFERENCES patient_carer_access (id)
);

CREATE TABLE documents (
                           id UUID PRIMARY KEY,
                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL,
                           version BIGINT NOT NULL,
                           patient_record_id UUID NOT NULL,
                           uploaded_by_user_id UUID NOT NULL,
                           status VARCHAR(50) NOT NULL,
                           original_file_name VARCHAR(255) NOT NULL,
                           stored_file_name VARCHAR(100) NOT NULL,
                           content_type VARCHAR(100) NOT NULL,
                           file_size BIGINT NOT NULL,
                           storage_path VARCHAR(500) NOT NULL,
                           document_type VARCHAR(50) NOT NULL,
                           processing_failure_reason VARCHAR(500),
                           CONSTRAINT uk_document_stored_file_name
                               UNIQUE (stored_file_name),
                           CONSTRAINT uk_document_storage_path
                               UNIQUE (storage_path),
                           CONSTRAINT fk_document_patient_record
                               FOREIGN KEY (patient_record_id)
                                   REFERENCES patient_records (id),
                           CONSTRAINT fk_document_uploaded_by
                               FOREIGN KEY (uploaded_by_user_id)
                                   REFERENCES users (id)
);

CREATE TABLE document_processing_results (
                                             id UUID PRIMARY KEY,
                                             created_at TIMESTAMPTZ NOT NULL,
                                             updated_at TIMESTAMPTZ NOT NULL,
                                             document_id UUID NOT NULL,
                                             extracted_text TEXT NOT NULL,
                                             machine_deidentified_text TEXT,
                                             approved_deidentified_text TEXT,
                                             appointment_date DATE,
                                             appointment_start_time TIME,
                                             appointment_end_time TIME,
                                             appointment_service VARCHAR(250),
                                             appointment_type VARCHAR(250),
                                             appointment_clinician_or_team VARCHAR(250),
                                             appointment_location_name VARCHAR(250),
                                             appointment_address_line_1 VARCHAR(150),
                                             appointment_address_line_2 VARCHAR(150),
                                             appointment_town_city VARCHAR(100),
                                             appointment_county VARCHAR(100),
                                             appointment_postcode VARCHAR(20),
                                             appointment_country VARCHAR(100),
                                             appointment_reviewed_by_user_id UUID,
                                             appointment_reviewed_at TIMESTAMPTZ,
                                             generated_summary TEXT,
                                             reviewed_summary TEXT,
                                             summary_source VARCHAR(30),
                                             processing_warning TEXT,
                                             processor_version VARCHAR(100) NOT NULL,
                                             model_name VARCHAR(255),
                                             prompt_version VARCHAR(100),
                                             deidentification_reviewed_by_user_id UUID,
                                             deidentification_reviewed_at TIMESTAMPTZ,
                                             summary_reviewed_by_user_id UUID,
                                             summary_reviewed_at TIMESTAMPTZ,
                                             CONSTRAINT uk_document_processing_result_document
                                                 UNIQUE (document_id),
                                             CONSTRAINT fk_document_processing_result_document
                                                 FOREIGN KEY (document_id)
                                                     REFERENCES documents (id),
                                             CONSTRAINT fk_processing_result_appointment_reviewer
                                                 FOREIGN KEY (appointment_reviewed_by_user_id)
                                                     REFERENCES users (id),
                                             CONSTRAINT fk_processing_result_deidentification_reviewer
                                                 FOREIGN KEY (deidentification_reviewed_by_user_id)
                                                     REFERENCES users (id),
                                             CONSTRAINT fk_processing_result_summary_reviewer
                                                 FOREIGN KEY (summary_reviewed_by_user_id)
                                                     REFERENCES users (id)
);

CREATE TABLE appointments (
                              id UUID PRIMARY KEY,
                              created_at TIMESTAMPTZ NOT NULL,
                              updated_at TIMESTAMPTZ NOT NULL,
                              archived_at TIMESTAMPTZ,
                              patient_record_id UUID NOT NULL,
                              appointment_date DATE NOT NULL,
                              start_time TIME NOT NULL,
                              end_time TIME,
                              service VARCHAR(250),
                              appointment_type VARCHAR(250),
                              clinician_or_team VARCHAR(250),
                              location_name VARCHAR(250),
                              location_address_line_1 VARCHAR(150),
                              location_address_line_2 VARCHAR(150),
                              location_town_city VARCHAR(100),
                              location_county VARCHAR(100),
                              location_postcode VARCHAR(20),
                              location_country VARCHAR(100),
                              notes VARCHAR(2000),
                              source_document_id UUID,
                              CONSTRAINT uk_appointment_source_document
                                  UNIQUE (source_document_id),
                              CONSTRAINT fk_appointment_patient_record
                                  FOREIGN KEY (patient_record_id)
                                      REFERENCES patient_records (id),
                              CONSTRAINT fk_appointment_source_document
                                  FOREIGN KEY (source_document_id)
                                      REFERENCES documents (id)
);

CREATE TABLE medications (
                             id UUID PRIMARY KEY,
                             created_at TIMESTAMPTZ NOT NULL,
                             updated_at TIMESTAMPTZ NOT NULL,
                             archived_at TIMESTAMPTZ,
                             patient_record_id UUID NOT NULL,
                             name VARCHAR(200) NOT NULL,
                             dose VARCHAR(100),
                             form VARCHAR(100),
                             instructions VARCHAR(500),
                             start_date DATE,
                             end_date DATE,
                             notes VARCHAR(2000),
                             CONSTRAINT fk_medication_patient_record
                                 FOREIGN KEY (patient_record_id)
                                     REFERENCES patient_records (id)
);

CREATE TABLE healthcare_contacts (
                                     id UUID PRIMARY KEY,
                                     created_at TIMESTAMPTZ NOT NULL,
                                     updated_at TIMESTAMPTZ NOT NULL,
                                     archived_at TIMESTAMPTZ,
                                     patient_record_id UUID NOT NULL,
                                     name VARCHAR(200) NOT NULL,
                                     role VARCHAR(150),
                                     organisation VARCHAR(200),
                                     phone_number VARCHAR(50),
                                     email VARCHAR(254),
                                     address_line_1 VARCHAR(150),
                                     address_line_2 VARCHAR(150),
                                     town_city VARCHAR(100),
                                     county VARCHAR(100),
                                     postcode VARCHAR(20),
                                     country VARCHAR(100),
                                     notes VARCHAR(2000),
                                     CONSTRAINT fk_healthcare_contact_patient_record
                                         FOREIGN KEY (patient_record_id)
                                             REFERENCES patient_records (id)
);

CREATE TABLE emergency_contacts (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMPTZ NOT NULL,
                                    updated_at TIMESTAMPTZ NOT NULL,
                                    archived_at TIMESTAMPTZ,
                                    patient_record_id UUID NOT NULL,
                                    name VARCHAR(200) NOT NULL,
                                    relationship VARCHAR(150) NOT NULL,
                                    phone_number VARCHAR(50) NOT NULL,
                                    alternative_phone_number VARCHAR(50),
                                    email VARCHAR(254),
                                    notes VARCHAR(2000),
                                    CONSTRAINT fk_emergency_contact_patient_record
                                        FOREIGN KEY (patient_record_id)
                                            REFERENCES patient_records (id)
);

CREATE TABLE blood_tests (
                             id UUID PRIMARY KEY,
                             created_at TIMESTAMPTZ NOT NULL,
                             updated_at TIMESTAMPTZ NOT NULL,
                             archived_at TIMESTAMPTZ,
                             patient_record_id UUID NOT NULL,
                             title VARCHAR(200),
                             test_date DATE NOT NULL,
                             provider VARCHAR(200),
                             notes VARCHAR(2000),
                             CONSTRAINT fk_blood_test_patient_record
                                 FOREIGN KEY (patient_record_id)
                                     REFERENCES patient_records (id)
);

CREATE TABLE blood_test_results (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMPTZ NOT NULL,
                                    updated_at TIMESTAMPTZ NOT NULL,
                                    blood_test_id UUID NOT NULL,
                                    analyte_name VARCHAR(200) NOT NULL,
                                    analyte_key VARCHAR(200) NOT NULL,
                                    result_value VARCHAR(100) NOT NULL,
                                    numeric_value NUMERIC(30, 10),
                                    unit VARCHAR(100),
                                    reference_range VARCHAR(150),
                                    result_flag VARCHAR(30),
                                    display_order INTEGER NOT NULL,
                                    CONSTRAINT fk_blood_test_result_blood_test
                                        FOREIGN KEY (blood_test_id)
                                            REFERENCES blood_tests (id)
);

CREATE TABLE medical_history_entries (
                                         id UUID PRIMARY KEY,
                                         created_at TIMESTAMPTZ NOT NULL,
                                         updated_at TIMESTAMPTZ NOT NULL,
                                         archived_at TIMESTAMPTZ,
                                         patient_record_id UUID NOT NULL,
                                         title VARCHAR(200) NOT NULL,
                                         summary TEXT NOT NULL,
                                         entry_date DATE NOT NULL,
                                         source_type VARCHAR(50) NOT NULL,
                                         source_document_id UUID,
                                         created_by_user_id UUID NOT NULL,
                                         CONSTRAINT uk_medical_history_source_document
                                             UNIQUE (source_document_id),
                                         CONSTRAINT fk_medical_history_patient_record
                                             FOREIGN KEY (patient_record_id)
                                                 REFERENCES patient_records (id),
                                         CONSTRAINT fk_medical_history_source_document
                                             FOREIGN KEY (source_document_id)
                                                 REFERENCES documents (id),
                                         CONSTRAINT fk_medical_history_created_by
                                             FOREIGN KEY (created_by_user_id)
                                                 REFERENCES users (id)
);

CREATE TABLE appointment_packs (
                                   id UUID PRIMARY KEY,
                                   created_at TIMESTAMPTZ NOT NULL,
                                   updated_at TIMESTAMPTZ NOT NULL,
                                   archived_at TIMESTAMPTZ,
                                   patient_record_id UUID NOT NULL,
                                   appointment_id UUID NOT NULL,
                                   title VARCHAR(250) NOT NULL,
                                   notes VARCHAR(2000),
                                   generated_by_user_id UUID NOT NULL,
                                   generated_at TIMESTAMPTZ NOT NULL,
                                   file_name VARCHAR(255) NOT NULL,
                                   stored_file_name VARCHAR(255) NOT NULL,
                                   storage_path VARCHAR(500) NOT NULL,
                                   content_type VARCHAR(100) NOT NULL,
                                   file_size BIGINT NOT NULL,
                                   CONSTRAINT fk_appointment_pack_patient_record
                                       FOREIGN KEY (patient_record_id)
                                           REFERENCES patient_records (id),
                                   CONSTRAINT fk_appointment_pack_appointment
                                       FOREIGN KEY (appointment_id)
                                           REFERENCES appointments (id),
                                   CONSTRAINT fk_appointment_pack_generated_by
                                       FOREIGN KEY (generated_by_user_id)
                                           REFERENCES users (id)
);

CREATE TABLE appointment_pack_items (
                                        id UUID PRIMARY KEY,
                                        created_at TIMESTAMPTZ NOT NULL,
                                        updated_at TIMESTAMPTZ NOT NULL,
                                        appointment_pack_id UUID NOT NULL,
                                        resource_type VARCHAR(50) NOT NULL,
                                        resource_id UUID NOT NULL,
                                        display_order INTEGER NOT NULL,
                                        CONSTRAINT uk_appointment_pack_item_resource
                                            UNIQUE (
                                                    appointment_pack_id,
                                                    resource_type,
                                                    resource_id
                                                ),
                                        CONSTRAINT fk_appointment_pack_item_pack
                                            FOREIGN KEY (appointment_pack_id)
                                                REFERENCES appointment_packs (id)
);

CREATE TABLE mfa_challenges (
                                id UUID PRIMARY KEY,
                                created_at TIMESTAMPTZ NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL,
                                user_id UUID NOT NULL,
                                expires_at TIMESTAMPTZ NOT NULL,
                                used BOOLEAN NOT NULL,
                                failed_attempts INTEGER NOT NULL,
                                CONSTRAINT fk_mfa_challenge_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users (id)
);

CREATE TABLE account_tokens (
                                id UUID PRIMARY KEY,
                                created_at TIMESTAMPTZ NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL,
                                user_id UUID NOT NULL,
                                purpose VARCHAR(50) NOT NULL,
                                token_hash VARCHAR(64) NOT NULL,
                                expires_at TIMESTAMPTZ NOT NULL,
                                used_at TIMESTAMPTZ,
                                invalidated_at TIMESTAMPTZ,
                                CONSTRAINT uk_account_token_hash
                                    UNIQUE (token_hash),
                                CONSTRAINT fk_account_token_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users (id)
);

CREATE TABLE patient_audit_events (
                                      id UUID PRIMARY KEY,
                                      source_event_id UUID NOT NULL,
                                      patient_record_id UUID NOT NULL,
                                      actor_user_id UUID NOT NULL,
                                      resource_type VARCHAR(50) NOT NULL,
                                      resource_id UUID NOT NULL,
                                      action VARCHAR(50) NOT NULL,
                                      occurred_at TIMESTAMPTZ NOT NULL,
                                      CONSTRAINT uk_patient_audit_source_event
                                          UNIQUE (source_event_id)
);

CREATE TABLE operational_events (
                                    id UUID PRIMARY KEY,
                                    source_event_id UUID NOT NULL,
                                    category VARCHAR(50) NOT NULL,
                                    event_name VARCHAR(100) NOT NULL,
                                    user_id UUID,
                                    occurred_at TIMESTAMPTZ NOT NULL,
                                    patient_resource_type VARCHAR(50),
                                    patient_action VARCHAR(50),
                                    authentication_action VARCHAR(50),
                                    authentication_outcome VARCHAR(50),
                                    document_type VARCHAR(50),
                                    processing_operation VARCHAR(50),
                                    processing_outcome VARCHAR(50),
                                    processing_failure_reason VARCHAR(50),
                                    duration_ms BIGINT,
                                    processor_version VARCHAR(100),
                                    model_name VARCHAR(100),
                                    prompt_version VARCHAR(100),
                                    page VARCHAR(50),
                                    CONSTRAINT uk_operational_event_source_event
                                        UNIQUE (source_event_id)
);

CREATE INDEX idx_patient_carer_access_patient
    ON patient_carer_access (patient_record_id);

CREATE INDEX idx_patient_carer_access_carer
    ON patient_carer_access (carer_user_id);

CREATE INDEX idx_documents_patient_record
    ON documents (patient_record_id);

CREATE INDEX idx_documents_status
    ON documents (status);

CREATE INDEX idx_blood_test_patient_date
    ON blood_tests (patient_record_id, test_date);

CREATE INDEX idx_blood_test_result_analyte_key
    ON blood_test_results (analyte_key);

CREATE INDEX idx_medical_history_patient_date
    ON medical_history_entries (
                                patient_record_id,
                                entry_date
        );

CREATE INDEX idx_account_token_user_purpose
    ON account_tokens (
                       user_id,
                       purpose
        );

CREATE INDEX idx_patient_audit_patient_occurred
    ON patient_audit_events (
                             patient_record_id,
                             occurred_at
        );

CREATE INDEX idx_patient_audit_actor
    ON patient_audit_events (actor_user_id);

CREATE INDEX idx_operational_event_occurred
    ON operational_events (occurred_at);

CREATE INDEX idx_operational_event_category_occurred
    ON operational_events (
                           category,
                           occurred_at
        );

CREATE INDEX idx_operational_event_name_occurred
    ON operational_events (
                           event_name,
                           occurred_at
        );

CREATE INDEX idx_operational_event_user_occurred
    ON operational_events (
                           user_id,
                           occurred_at
        );