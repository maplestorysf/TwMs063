/*
 結構撰寫By 宗達
 */

/* global cm */

var status = -1;
var Editing = false // true = 維修中， false = 開放。
var msg = "";


function start() {
    if (status = -1) {
       msg = "歡迎來玩 辛巴谷Ver:113\r\n"+
			"熱門時段人數都是破千的喔\r\n";
        cm.sendNext(msg);
        cm.dispose();
        return;
    }
  
}


