-- RailReserve seed data. Idempotent: safe to re-run.

INSERT INTO users (username, password_hash) VALUES
    ('demo',
     'pbkdf2$120000$Z3Rbwq30pQbD0Fu14RuvAA==$R8b/g9NRInrNL3MddmTOBseuoE5Kd/SYB/V6Zz4wyx0=')
ON CONFLICT (username) DO NOTHING;   -- password for 'demo' is: demo123

INSERT INTO trains (train_number, train_name) VALUES
    (12009, 'Ahmedabad-Mumbai Central Shatabdi Express'),
    (12301, 'Howrah Rajdhani Express'),
    (12621, 'Tamil Nadu Express'),
    (12715, 'Sachkhand Express'),
    (12951, 'Mumbai Rajdhani Express'),
    (12961, 'Avantika Express'),
    (16526, 'Kanyakumari Express'),
    (16787, 'Tirukural Express'),
    (18507, 'Visakhapatnam Intercity Express'),
    (22691, 'Rajdhani Express')
ON CONFLICT (train_number) DO NOTHING;
