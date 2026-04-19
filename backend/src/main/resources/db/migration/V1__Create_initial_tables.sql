CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       telegram_chat_id BIGINT,
                       is_active BOOLEAN DEFAULT true,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE medicines (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(100) NOT NULL,
                           dosage VARCHAR(50),
                           description TEXT,
                           instructions TEXT,
                           is_active BOOLEAN DEFAULT true,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reminders (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           medicine_id BIGINT NOT NULL REFERENCES medicines(id),
                           reminder_time TIME NOT NULL,
                           is_active BOOLEAN DEFAULT true,
                           days_of_week VARCHAR(50) DEFAULT 'everyday',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE medicine_history (
                                  id BIGSERIAL PRIMARY KEY,
                                  reminder_id BIGINT NOT NULL REFERENCES reminders(id),
                                  scheduled_time TIMESTAMP NOT NULL,
                                  taken_at TIMESTAMP,
                                  status VARCHAR(20) DEFAULT 'PENDING',
                                  notes TEXT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- course_medications
CREATE TABLE course_medications (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(255),
    description TEXT,
    instructions TEXT,
    meal_mode VARCHAR(50),
    time_of_day TIME NOT NULL,
    schedule_type VARCHAR(50),
    interval_days INT,
    generated_medicine_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
ALTER TABLE reminders ADD COLUMN course_medication_id BIGINT REFERENCES course_medications(id);
ALTER TABLE medicine_history
  DROP CONSTRAINT medicine_history_reminder_id_fkey,
  ADD CONSTRAINT medicine_history_reminder_id_fkey
      FOREIGN KEY (reminder_id) REFERENCES reminders(id) ON DELETE CASCADE;
ALTER TABLE reminders ADD COLUMN specific_date DATE;
CREATE INDEX idx_reminders_specific_date ON reminders(specific_date) WHERE specific_date IS NOT NULL;

-- Для поиска активных напоминаний
CREATE INDEX idx_reminders_active ON reminders(is_active) WHERE is_active = true;

-- Для связи с course_medication
CREATE INDEX idx_reminders_course_medication ON reminders(course_medication_id) WHERE course_medication_id IS NOT NULL;