INSERT INTO patients (id, admission_number, name, diagnosis, current_department, nursing_level, isolation_requirement, status, admission_time) VALUES
(10001, 'P20240001', 'Zhang San', 'Acute Appendicitis', 'Emergency', 'LEVEL2', 'NONE', 'STABLE', '2024-01-15 08:00:00'),
(10002, 'P20240002', 'Li Si', 'Severe Pneumonia', 'Respiratory', 'SPECIAL', 'AIRBORNE', 'CRITICAL', '2024-01-16 10:30:00'),
(10003, 'P20240003', 'Wang Wu', 'Post Fracture Surgery', 'Orthopedics', 'LEVEL1', 'NONE', 'POSTOPERATIVE', '2024-01-17 14:00:00'),
(10004, 'P20240004', 'Zhao Liu', 'Stroke', 'Neurology', 'LEVEL1', 'NONE', 'CRITICAL', '2024-01-18 09:15:00'),
(10005, 'P20240005', 'Sun Qi', 'Myocardial Infarction', 'Cardiology', 'SPECIAL', 'NONE', 'CRITICAL', '2024-01-19 07:45:00');

INSERT INTO beds (id, bed_number, department, bed_type, isolation_type, occupied, enabled) VALUES
(10001, 'ICU-001', 'ICU', 'ICU', 'NONE', false, true),
(10002, 'ICU-002', 'ICU', 'ICU', 'NONE', false, true),
(10003, 'ICU-003', 'ICU', 'ICU', 'AIRBORNE', false, true),
(10004, 'GEN-SURG-001', 'GeneralSurgery', 'GENERAL', 'NONE', false, true),
(10005, 'GEN-SURG-002', 'GeneralSurgery', 'GENERAL', 'NONE', false, true),
(10006, 'GEN-SURG-003', 'GeneralSurgery', 'GENERAL', 'CONTACT', false, true),
(10007, 'NEURO-001', 'Neurology', 'GENERAL', 'NONE', false, true),
(10008, 'NEURO-002', 'Neurology', 'GENERAL', 'NONE', false, true),
(10009, 'CARDIO-001', 'Cardiology', 'GENERAL', 'NONE', false, true),
(10010, 'CARDIO-002', 'Cardiology', 'ICU', 'NONE', false, true),
(10011, 'RESPIR-001', 'Respiratory', 'GENERAL', 'AIRBORNE', false, true),
(10012, 'RESPIR-002', 'Respiratory', 'GENERAL', 'NONE', false, true),
(10013, 'ORTHO-001', 'Orthopedics', 'GENERAL', 'NONE', false, true),
(10014, 'ORTHO-002', 'Orthopedics', 'GENERAL', 'NONE', false, true),
(10015, 'PED-001', 'Pediatrics', 'PEDIATRIC', 'NONE', false, true),
(10016, 'ISO-001', 'InfectiousDisease', 'ISOLATION', 'CONTACT', false, true),
(10017, 'ISO-002', 'InfectiousDisease', 'ISOLATION', 'DROPLET', false, true),
(10018, 'ISO-003', 'InfectiousDisease', 'ISOLATION', 'AIRBORNE', false, true);
