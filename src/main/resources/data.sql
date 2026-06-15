INSERT INTO patients (id, admission_number, name, diagnosis, current_department, nursing_level, isolation_requirement, status, admission_time) VALUES
(10001, 'P20240001', 'Zhang San', 'Acute Appendicitis', 'Emergency', 'LEVEL2', 'NONE', 'STABLE', '2024-01-15 08:00:00'),
(10002, 'P20240002', 'Li Si', 'Severe Pneumonia', 'Respiratory', 'SPECIAL', 'AIRBORNE', 'CRITICAL', '2024-01-16 10:30:00'),
(10003, 'P20240003', 'Wang Wu', 'Post Fracture Surgery', 'Orthopedics', 'LEVEL1', 'NONE', 'POSTOPERATIVE', '2024-01-17 14:00:00'),
(10004, 'P20240004', 'Zhao Liu', 'Stroke', 'Neurology', 'LEVEL1', 'NONE', 'CRITICAL', '2024-01-18 09:15:00'),
(10005, 'P20240005', 'Sun Qi', 'Myocardial Infarction', 'Cardiology', 'SPECIAL', 'NONE', 'CRITICAL', '2024-01-19 07:45:00');

INSERT INTO beds (id, bed_number, department, bed_type, isolation_type, occupied, enabled, room_number, ward_number) VALUES
(10001, 'ICU-001', 'ICU', 'ICU', 'NONE', false, true, 'ICU-ROOM-1', 'ICU-WARD-A'),
(10002, 'ICU-002', 'ICU', 'ICU', 'NONE', false, true, 'ICU-ROOM-1', 'ICU-WARD-A'),
(10003, 'ICU-003', 'ICU', 'ICU', 'AIRBORNE', false, true, 'ICU-ROOM-2', 'ICU-WARD-B'),
(10004, 'GEN-SURG-001', 'GeneralSurgery', 'GENERAL', 'NONE', false, true, 'GS-ROOM-1', 'GS-WARD-A'),
(10005, 'GEN-SURG-002', 'GeneralSurgery', 'GENERAL', 'NONE', false, true, 'GS-ROOM-1', 'GS-WARD-A'),
(10006, 'GEN-SURG-003', 'GeneralSurgery', 'GENERAL', 'CONTACT', false, true, 'GS-ROOM-2', 'GS-WARD-B'),
(10007, 'NEURO-001', 'Neurology', 'GENERAL', 'NONE', false, true, 'NEURO-ROOM-1', 'NEURO-WARD-A'),
(10008, 'NEURO-002', 'Neurology', 'GENERAL', 'NONE', false, true, 'NEURO-ROOM-1', 'NEURO-WARD-A'),
(10009, 'CARDIO-001', 'Cardiology', 'GENERAL', 'NONE', false, true, 'CARDIO-ROOM-1', 'CARDIO-WARD-A'),
(10010, 'CARDIO-002', 'Cardiology', 'ICU', 'NONE', false, true, 'CARDIO-ROOM-2', 'CARDIO-WARD-B'),
(10011, 'RESPIR-001', 'Respiratory', 'GENERAL', 'AIRBORNE', false, true, 'RESPIR-ROOM-1', 'RESPIR-WARD-B'),
(10012, 'RESPIR-002', 'Respiratory', 'GENERAL', 'NONE', false, true, 'RESPIR-ROOM-2', 'RESPIR-WARD-A'),
(10013, 'ORTHO-001', 'Orthopedics', 'GENERAL', 'NONE', false, true, 'ORTHO-ROOM-1', 'ORTHO-WARD-A'),
(10014, 'ORTHO-002', 'Orthopedics', 'GENERAL', 'NONE', false, true, 'ORTHO-ROOM-1', 'ORTHO-WARD-A'),
(10015, 'PED-001', 'Pediatrics', 'PEDIATRIC', 'NONE', false, true, 'PED-ROOM-1', 'PED-WARD-A'),
(10016, 'ISO-001', 'InfectiousDisease', 'ISOLATION', 'CONTACT', false, true, 'ISO-ROOM-1', 'ISO-WARD-A'),
(10017, 'ISO-002', 'InfectiousDisease', 'ISOLATION', 'DROPLET', false, true, 'ISO-ROOM-2', 'ISO-WARD-A'),
(10018, 'ISO-003', 'InfectiousDisease', 'ISOLATION', 'AIRBORNE', false, true, 'ISO-ROOM-3', 'ISO-WARD-B');
