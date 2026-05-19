(ns audio-feedback.config)

(defonce extension-api (atom nil))

(defn get-config [k default-val]
(if @extension-api
(let [val (.get (.-settings @extension-api) k)]
  (if (nil? val) default-val val))
default-val))

(defn set-config [k val]
(when @extension-api
(.set (.-settings @extension-api) k val)))

(defn register-settings! []
(when-let [api @extension-api]
(let [general-fields
      #js [#js {:id          "general-heading"
                :name        "GENERAL"
                :description ""
                :action      #js {:type "heading"}}
           #js {:id          "enabled"
                :name        "Enable Audio Feedback"
                :description "Master switch — overrides all settings below"
                :action      #js {:type "switch"}
                :default     true}
           #js {:id          "volume"
                :name        "Master Volume"
                :description "0.0 to 1.0 (e.g. 0.4)"
                :action      #js {:type "input" :placeholder "0.4"}
                :default     "0.4"}
           #js {:id          "theme"
                :name        "Sound Theme"
                :description "Zen · Retro · Halo · Custom (use your own Firebase URLs)"
                :action      #js {:type    "select"
                                  :items   #js ["Zen" "Retro" "Halo" "Custom"]}
                :default     "Zen"}]
      sound-type-fields
      #js [#js {:id          "sound-types-heading"
                :name        "SOUND TYPES"
                :description ""
                :action      #js {:type "heading"}}
           #js {:id          "todo-enabled"
                :name        "TODO Sounds"
                :description "Play sounds on checkbox toggles"
                :action      #js {:type "switch"}
                :default     true}
           #js {:id          "nav-enabled"
                :name        "Navigation Sounds"
                :description "Play sounds when zooming or opening sidebar"
                :action      #js {:type "switch"}
                :default     true}
           #js {:id          "hierarchy-enabled"
                :name        "Hierarchy Sounds"
                :description "Play sounds when expanding or collapsing"
                :action      #js {:type "switch"}
                :default     true}]
      url-fields
      #js [#js {:id          "custom-urls-heading"
                :name        "CUSTOM SOUND URLs"
                :description "Active when Custom theme is selected. Paste Firebase storage URLs (must include ?alt=media token)."
                :action      #js {:type "heading"}}
           #js {:id          "custom-sound-todo-done"
                :name        "TODO Done"
                :description "Sound URL for checking off a TODO"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}
           #js {:id          "custom-sound-collapse"
                :name        "Collapse"
                :description "Sound URL for collapsing a block"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}
           #js {:id          "custom-sound-expand"
                :name        "Expand"
                :description "Sound URL for expanding a block"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}
           #js {:id          "custom-sound-indent"
                :name        "Indent"
                :description "Sound URL for indenting a block"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}
           #js {:id          "custom-sound-outdent"
                :name        "Outdent"
                :description "Sound URL for outdenting a block"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}
           #js {:id          "custom-sound-navigate"
                :name        "Navigate"
                :description "Sound URL for page navigation"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}
           #js {:id          "custom-sound-sidebar-open"
                :name        "Sidebar Open"
                :description "Sound URL for opening the sidebar"
                :action      #js {:type "input" :placeholder "https://firebasestorage.googleapis.com/..."}
                :default     ""}]
      all-fields (js/Array.from
                   (.concat general-fields sound-type-fields url-fields))
      panel-config #js {:tabTitle "Audio Feedback"
                         :settings all-fields}]
  (.create (.-panel (.-settings api)) panel-config))))
