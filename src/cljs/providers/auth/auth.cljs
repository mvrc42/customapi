(ns providers.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [app.state :refer [app-state]]
            [providers.cookies :as cookies]))

(defn retrieve-session
  "Retry to connect from an od session stored in cookies"
  []
  (let [creds (cookies/get :session)]
    (when creds
      (go (let [response (<! (http/post "/api/login" {:form-params creds}))]
        (when (:success response)
          (swap! app-state assoc :projects (get-in response [:body :projects])
                                 :creds creds
                                 :connected true)))))))

(defn login
  "Log a user with AWS credentials"
  [remember loading error]
  (reset! loading true)
  (go (let [response (<! (http/post "/api/login" {:form-params (get @app-state :creds)}))]
    (if (:success response)
      ;login success
      (do
        (swap! app-state assoc :projects (get-in response [:body :projects])
                               :page :projects
                               :connected true)
        (when remember
          (cookies/set :session (get @app-state :creds))))
      ;login failed
      (do (swap! app-state assoc-in [:creds :secret-key] "")
        (reset! error true)))
    (reset! loading false))))

(defn logout
  "Log the user out"
  []
  (do
    (cookies/remove :session)
    (swap! app-state dissoc :creds :connected :projects)))
