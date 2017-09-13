(ns clj-test.core
  (:require
    [chime :refer [chime-at]]
    [clj-time.core :as t]
    [clj-time.periodic :refer [periodic-seq]]

    ;; WEB
    [ring.middleware.params :as params]
    [immutant.web :as web]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [clj-http.client :as http-client]
    [clojure.data.json :as json]

    ;; DB:
    [yesql.core :as yesql]

    ;; DEV:
    [ring.middleware.reload :as reload]
    ))



;; DB:
(yesql/defqueries "clj_test/query.sql"
  {:connection {:classname "com.mysql.jdbc.Driver"
                :subprotocol "mysql"
                :subname "//localhost:3306/project"
                :user "root"
                :password "root"}})



(defn current-game-state []
  (json/read-json (:body (http-client/get "https://abdur.cc/kenodata2/v2/99908"))))


(defn round-not-finished? [game_state]
  (empty? (:balls (:game game_state))))


(defn current-round-number? [expected_round_number game_state]
  (= expected_round_number (:round (:game game_state))))


(defn get-winning-coeff [matched_balls_count selected_balls_count]
  (def winning-coeff-matrix
    [[0 0 0 0 0 0 0 0 0 0]
     [3 1 0 0 0 0 0 0 0 0]
     [0 15 2 1 1 0 0 0 0 0]
     [0 0 55 10 3 2 2 0 0 0]
     [0 0 0 80 20 15 4 5 2 0]
     [0 0 0 0 150 60 20 15 10 5]
     [0 0 0 0 0 500 80 50 25 30]
     [0 0 0 0 0 0 1000 200 125 100]
     [0 0 0 0 0 0 0 2000 1000 300]
     [0 0 0 0 0 0 0 0 5000 2000]
     [0 0 0 0 0 0 0 0 0 10000]])
  (nth (nth winning-coeff-matrix matched_balls_count) (- selected_balls_count 1)))
;; (get-winning-coeff 9 10)


(defn number-of-matches [winning_numbers selected_numbers]
  (let [winning_numbers_repetitions (frequencies winning_numbers)]
    (reduce #(+ %1 (get winning_numbers_repetitions %2 0)) 0 selected_numbers)))
;; (number-of-matches [1 2 3 4 1 1 2] [1])


(defn calculate-bets-for-round [round_number winning_numbers]
  (let [bets (raw-select-bets-by-round-number {:round_number round_number})
        ]
    (run! (fn [bet]
            (let [selected_numbers (json/read-str (:selected_numbers bet))
                  selected_balls_count (count selected_numbers)
                  matched_balls_count (number-of-matches winning_numbers selected_numbers)
                  coeff (get-winning-coeff matched_balls_count selected_balls_count)
                  bet_amount (:bet_amount bet)
                  bet_award (* coeff bet_amount)
                  ]
              (prn "calc for bet: " bet)
              (try
                (raw-update-bet! {:unique_number (:unique_number bet)
                                  :calculating_award_datetime (new java.sql.Timestamp (System/currentTimeMillis))
                                  :bet_award bet_award})
                (catch Exception e
                  (prn e)))
              ))
          bets)
    ))
;; (calculate-bets-for-round 1 [1 2 100])


;; Упрощение. Сервер будет начинать работу после первого сыгранного раунда.
(def last_round (atom -1))
(defn check-round-end []
  (let [game (:game (current-game-state))
        round (:round game)
        balls (map #(Long/parseLong %) (:balls game))
        ]
    (when (and (not= @last_round round) (not-empty balls))
      (prn "@last_round=" @last_round)
      (raw-inser-round! {:round_number (+ 1 round)})
      (try
        (raw-update-round! {:round_number round :balls (json/write-str balls)})
        (future
          (do
            (prn "start calulation bet wind.")
            (calculate-bets-for-round round balls)))
        (catch Exception e
          (prn e)))
      (reset! last_round round))
    ))
;; (check-round-end)
;; (prn @last_round)
;; (reset! last_round -1)


(defn check-round-scheduler []
  (chime-at (periodic-seq (t/now) (-> 1 t/seconds))
            (fn [_time]
              (check-round-end))))
;; (def sh (check-round-scheduler))
;; (sh)


(def merge-with+
  (partial merge-with +))


(defn ball-amount-pairs-per-bet [bet_record]
  (let [selected_balls (json/read-str (:selected_numbers bet_record))
        bet_amount (:bet_amount bet_record)]
    (zipmap selected_balls (repeat bet_amount))))


(defn ball-amount-pairs-per-bet-for-round [round_number]
  (let [bet_records (raw-select-bets-by-round-number {:round_number round_number})
        by_bet_ball_amount_list (map ball-amount-pairs-per-bet bet_records)]
    (apply merge-with+ by_bet_ball_amount_list)))
;; (current-balls-bet-amount-for-round 1)


(defn total-current-game-state []
  (let [current_game_state (current-game-state)
        game (:game current_game_state)
        external_balls_amount_pair (reduce-kv #(assoc %1 (Long/parseLong (name %2)) (Long/parseLong %3)) {} (:bets current_game_state))
        current_balls_amount_pair (ball-amount-pairs-per-bet-for-round (:round game))
        total_bets (merge-with + external_balls_amount_pair current_balls_amount_pair)]
    {:game game :bets total_bets}))
;; (total-current-game-state)


(defn try-accept-bet [unique_number round_number client_id selected_numbers bet_amount]
  (let [current_state (current-game-state)
        round_not_finished (round-not-finished? current_state)
        current_round_number (current-round-number? round_number current_state)]
    (when (and round_not_finished current_round_number)
      (raw-inser-bet! {:unique_number unique_number :round_number round_number :client_id client_id :selected_numbers selected_numbers :bet_amount bet_amount}))))
(try-accept-bet 13 355124 1 "[1,2,3,100]" 100)
;; (try-accept-bet 102,355126,1,"[1,2,3,4,5,6,7,8,9,10]",100)
(Integer/parseInt "1")


(defn prepare-bet [bets_records]
  (map #(select-keys % [:unique_number :round_number :selected_numbers :bet_amount :bet_award]) bets_records))
;; (prn (prepare-bet (raw-select-bets-by-client-id {:client_id 1})))


(defn games-status-with-client-bets [client_id]
  (let [game_state (total-current-game-state)
        round_number (:round_number game_state)
        bets_records (prepare-bet (raw-select-bets-by-client-id {:client_id client_id}))
        bets_by_round (into (sorted-map-by >) (group-by :round_number bets_records))]
    {:bets_by_round bets_by_round :game_state game_state}))
;; (prn (games-status-with-client-bets 1))


(defn parseInt [i]
  (Integer/parseInt i))



(defroutes app
  (GET "/get_state/:client_id" [client_id]
       (json/write-str (games-status-with-client-bets client_id)))
  ;; localhost:8080/accept_bet?unique_number=141&round_number=355197&client_id=1&selected_numbers=[1,2,3,4,5,6]&bet_amount=100
  ;; localhost:8080/accept_bet?unique_number=142&round_number=355197&client_id=1&selected_numbers=[7,8,9,10,11,12]&bet_amount=100
  ;; localhost:8080/accept_bet?unique_number=143&round_number=355197&client_id=1&selected_numbers=[13,14,15,16,17,18]&bet_amount=100
  ;; localhost:8080/accept_bet?unique_number=144&round_number=355197&client_id=1&selected_numbers=[19,20,21,22,23,24]&bet_amount=100
  (GET "/accept_bet" [unique_number round_number client_id selected_numbers bet_amount]
       (future (try-accept-bet (parseInt unique_number) (parseInt round_number) (parseInt  client_id) selected_numbers (parseInt bet_amount)))
       "accepted")
  (route/not-found "<h1>Page not found</h1>"))


(def http
  (-> app
      params/wrap-params))


(defn -main []
  (check-round-scheduler)
  (web/run (reload/wrap-reload #'http) {:port 8080}))
