(ns ^:figwheel-always zelkova-searcher.core
  (:require [reagent.core :as reagent]
            [zelkova-searcher.jsonp :as jsonp]
            [jamesmacaulay.zelkova.signal :as z]
            [jamesmacaulay.zelkova.time :as time]
            [cljs.core.async :as async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defonce app-state (reagent/atom {:text "" :suggestions []}))

(defn search-request
  [text]
  {:url "http://en.wikipedia.org/w/api.php"
   :params {:action "opensearch"
            :search text
            :format "json"}})

(defn channel-input-handler
  [ch]
  (fn [e]
    (async/put! ch (.. e -target -value))))

(defn suggestions-signal
  [text-input]
  (->> text-input
       (time/debounce 200)
       (z/map search-request)
       (jsonp/fetch-responses-drop-stale)
       (z/map second)))

(defn merge-channel-values-into-atom!
  [atom ch]
  (go-loop []
    (when-let [diff (<! ch)]
      (swap! atom merge diff)
      (recur))))

(defn search-view
  [{:keys [text]}]
  (let [text-input  (z/write-port text)
        suggestions (suggestions-signal text-input)
        updates-signal (z/indexed-updates
                         {:text text-input
                          :suggestions suggestions})
        input-change-handler (channel-input-handler text-input)]
    (->> updates-signal
         (z/to-chan)
         (merge-channel-values-into-atom! app-state))
    (fn [{:keys [suggestions]}]
      [:div.row
       [:div.col-md-4]
       [:div.col-md-4
        [:label {:for "search-input"}
         "Search Wikipedia"]
        [:input {:class "form-control"
                 :name  "search-input"
                 :type  "text"
                 :on-change input-change-handler}]
        [:ul.list-unstyled
         (for [title suggestions]
           [:li {:key title} title])]]
       [:div.col-md-4]])))

(defn root-view
  []
  [search-view @app-state])

(reagent/render-component [root-view] (. js/document (getElementById "app")))
