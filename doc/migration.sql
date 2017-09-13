/*
Создание индексов и другие оптимизации не проводились целенаправленно.
*/


CREATE DATABASE project;
# DROP DATABASE project;

USE project;



# -------- ROUNDS --------
CREATE TABLE project.rounds (
  round_number int,
  balls JSON,
  PRIMARY KEY (round_number)
);



# -------- BETS --------
/*
Сделано допущение что от клиента приходит всегда уникальный id для каждой ставки.
В реальной системе id иногда могут повторятся, все зависит от соглашений, реализации, и необходимо специально обсуждать отталкиваясь от того как работает система в момент обсуждения.
Поскольку в задании написано что номер уникальный это интерпретируется буквально, по этому уникальный уникальный используется как PK, с именем unique_number.
Он же обеспечивает идемпотентность запросов на добавление ставки, т.к. первичный ключ по умолчанию имеет ограничение уникальности(unique constraint).
*/
CREATE TABLE project.bets (
  unique_number int,
  round_number int NOT NULL,
  client_id int NOT NULL,
  accepting_bet_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  selected_numbers JSON NOT NULL,
  bet_amount int NOT NULL,
  calculating_award_datetime TIMESTAMP NULL DEFAULT NULL,
  bet_award int,
  PRIMARY KEY (unique_number),
  FOREIGN KEY (round_number) REFERENCES rounds(round_number)
);

# DROP TABLE  project.bets;
# INSERT INTO bets (unique_number, selected_numbers, bet_amount) VALUES (1, "[1, 2, 3, 4]", 100);

