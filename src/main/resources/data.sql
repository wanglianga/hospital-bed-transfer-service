INSERT INTO patients (id, admission_number, name, diagnosis, current_department, nursing_level, isolation_requirement, status, admission_time) VALUES
(1, 'P20240001', 'Zhang San', 'Acute Appendicitis', 'Emergency', 'LEVEL2', 'NONE', 'STABLE', '2024-01-15 08:00:00'),
(2, 'P20240002', 'Li Si', 'Severe Pneumonia', 'Respiratory', 'SPECIAL', 'AIRBORNE', 'CRITICAL', '2024-01-16 10:30:00'),
(3, 'P20240003', 'Wang Wu', 'Post Fracture Surgery', 'Orthopedics', 'LEVEL1', 'NONE', 'POSTOPERATIVE', '2024-01-17 14:00:00'),
(4, 'P20240004', 'Zhao Liu', 'Stroke', 'Neurology', 'LEVEL1', 'NONE', 'CRITICAL', '2024-01-18 09:15:00'),
(5, 'P20240005', 'Sun Qi', 'Myocardial Infarction', 'Cardiology', 'SPECIAL', 'NONE', 'CRITICAL', '2024-01-19 07:45:00');

INSERT INTO beds (id, bed_number, department, bed_type, isolation_type, occupied, enabled) VALUES
(1, 'ICU-001', 'ICU', 'ICU', 'NONE', false, true),
(2, 'ICU-002', 'ICU', 'ICU', 'NONE', false, true),
(3, 'ICU-003', 'ICU', 'ICU', 'AIRBORNE', false, true),
(4, 'GEN-SURG-001', 'GeneralSurgery', 'GENERAL', 'NONE', false, true),
(5, 'GEN-SURG-002', 'GeneralSurgery', 'GENERAL', 'NONE', false, true),
(6, 'GEN-SURG-003', 'GeneralSurgery', 'GENERAL', 'CONTACT', false, true),
(7, 'NEURO-001', 'Neurology', 'GENERAL', 'NONE', false, true),
(8, 'NEURO-002', 'Neurology', 'GENERAL', 'NONE', false, true),
(9, 'CARDIO-001', 'Cardiology', 'GENERAL', 'NONE', false, true),
(10, 'CARDIO-002', 'Cardiology', 'ICU', 'NONE', false, true),
(11, 'RESPIR-001', 'Respiratory', 'GENERAL', 'AIRBORNE', false, true),
(12, 'RESPIR-002', 'Respiratory', 'GENERAL', 'NONE', false, true),
(13, 'ORTHO-001', 'Orthopedics', 'GENERAL', 'NONE', false, true),
(14, 'ORTHO-002', 'Orthopedics', 'GENERAL', 'NONE', false, true),
(15, 'PED-001', 'Pediatrics', 'PEDIATRIC', 'NONE', false, true),
(16, 'ISO-001', 'InfectiousDisease', 'ISOLATION', 'CONTACT', false, true),
(17, 'ISO-002', 'InfectiousDisease', 'ISOLATION', 'DROPLET', false, true),
(18, 'ISO-003', 'InfectiousDisease', 'ISOLATION', 'AIRBORNE', false, true);
