define("ace/theme/logfile", ["require", "exports", "module", "ace/lib/dom"], function (require, exports, module) {

    exports.isDark = true;
    exports.cssClass = "ace-logfile";
    exports.cssText = ".ace-logfile .ace_gutter {\
background: #1a1a1a;\
color: #DEDEDE\
}\
.ace-logfile .ace_print-margin {\
width: 1px;\
background: #1a1a1a\
}\
.ace-logfile {\
background-color: #000000;\
color: #DEDEDE\
}\
.ace-logfile .ace_cursor {\
color: #9F9F9F\
}\
.ace-logfile .ace_marker-layer .ace_selection {\
background: #424242\
}\
.ace-logfile.ace_multiselect .ace_selection.ace_start {\
box-shadow: 0 0 3px 0px #000000;\
border-radius: 2px\
}\
.ace-logfile .ace_marker-layer .ace_step {\
background: rgb(102, 82, 0)\
}\
.ace-logfile .ace_marker-layer .ace_bracket {\
margin: -1px 0 0 -1px;\
border: 1px solid #888888\
}\
.ace-logfile .ace_marker-layer .ace_highlight {\
border: 1px solid rgb(110, 119, 0);\
border-bottom: 0;\
box-shadow: inset 0 -1px rgb(110, 119, 0);\
margin: -1px 0 0 -1px;\
background: rgba(255, 235, 0, 0.1);\
}\
.ace-logfile .ace_marker-layer .ace_active-line {\
background: #2A2A2A\
}\
.ace-logfile .ace_gutter-active-line {\
background-color: #2A2A2A\
}\
.ace-logfile .ace_stack {\
background-color: rgb(66, 90, 44)\
}\
.ace-logfile .ace_marker-layer .ace_selected-word {\
border: 1px solid #888888\
}\
.ace-logfile .ace_invisible {\
color: #343434\
}\
.ace-logfile .ace_keyword,\
.ace-logfile .ace_meta,\
.ace-logfile .ace_storage,\
.ace-logfile .ace_storage.ace_type,\
.ace-logfile .ace_support.ace_type {\
color: #C397D8\
}\
.ace-logfile .ace_keyword.ace_operator {\
color: #70C0B1\
}\
.ace-logfile .ace_constant.ace_character,\
.ace-logfile .ace_constant.ace_language,\
.ace-logfile .ace_constant.ace_numeric,\
.ace-logfile .ace_keyword.ace_other.ace_unit,\
.ace-logfile .ace_support.ace_constant,\
.ace-logfile .ace_variable.ace_parameter {\
color: #E78C45\
}\
.ace-logfile .ace_constant.ace_other {\
color: #EEEEEE\
}\
.ace-logfile .ace_invalid {\
color: #CED2CF;\
background-color: #DF5F5F\
}\
.ace-logfile .ace_invalid.ace_deprecated {\
color: #CED2CF;\
background-color: #B798BF\
}\
.ace-logfile .ace_fold {\
background-color: #7AA6DA;\
border-color: #DEDEDE\
}\
.ace-logfile .ace_entity.ace_name.ace_function,\
.ace-logfile .ace_support.ace_function,\
.ace-logfile .ace_variable {\
color: #7AA6DA\
}\
.ace-logfile .ace_support.ace_class,\
.ace-logfile .ace_support.ace_type {\
color: #E7C547\
}\
.ace-logfile .ace_heading,\
.ace-logfile .ace_markup.ace_heading,\
.ace-logfile .ace_string {\
color: #B9CA4A\
}\
.ace-logfile .ace_entity.ace_name.ace_tag,\
.ace-logfile .ace_entity.ace_other.ace_attribute-name,\
.ace-logfile .ace_meta.ace_tag,\
.ace-logfile .ace_string.ace_regexp,\
.ace-logfile .ace_variable {\
color: #D54E53\
}\
.ace-logfile .ace_comment {\
color: #969896\
}\
.ace-logfile .ace_c9searchresults.ace_keyword {\
color: #C2C280;\
}\
.ace-logfile .ace_indent-guide {\
background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAACCAYAAACZgbYnAAAAEklEQVQImWNgYGBgYFBXV/8PAAJoAXX4kT2EAAAAAElFTkSuQmCC) right repeat-y\
}\
.ace-logfile .ace_timestamp {\
color: #79ceed;\
}\
.ace-logfile .ace_category {\
color: #B9CA4A;\
}\
.ace-logfile .ace_level.ace_error {\
color: #c82e2e;\
}\
.ace-logfile .ace_level.ace_warn {\
color: #FFBC11;\
}\
.ace-logfile .ace_level.ace_info {\
color: inherit;\
}\
.ace-logfile .ace_level.ace_debug {\
color: #666666;\
}\
.ace-logfile .ace_exception {\
color: #FFBC11;\
background-color: #c82e2e;\
}";

    var dom = require("../lib/dom");
    dom.importCssString(exports.cssText, exports.cssClass);
});