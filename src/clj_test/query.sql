-- name: raw-select-round-by-id
SELECT * FROM rounds WHERE round_number = :round_number;

-- name: raw-inser-round!
INSERT INTO rounds (round_number) VALUES (:round_number);

-- name: raw-update-round!
UPDATE rounds set balls = :balls WHERE round_number = :round_number;



-- name: raw-select-bets
SELECT * FROM bets;

-- name: raw-select-bets-by-client-id
SELECT * FROM bets WHERE client_id = :client_id;

-- name: raw-select-bets-by-round-number
SELECT * FROM bets WHERE round_number = :round_number;

-- name: raw-inser-bet!
INSERT INTO bets (unique_number, round_number, client_id, selected_numbers, bet_amount) VALUES (:unique_number, :round_number, :client_id, :selected_numbers, :bet_amount);

-- name: raw-update-bet!
UPDATE bets set calculating_award_datetime = :calculating_award_datetime, bet_award = :bet_award WHERE unique_number = :unique_number;
