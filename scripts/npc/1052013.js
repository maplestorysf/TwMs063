var maps = Array(190000000, 190000001, 190000002, 191000000, 191000001, 192000000, 192000001, 195000000, 195010000, 195020000, 195030000, 196000000, 196010000, 196000000, 197000000, 197010000);

function start() {
    var selStr = "選擇一個想要一個練功點。#b";
    for (var i = 0; i < maps.length; i++) {
	selStr += "\r\n#L" + i + "##m" + maps[i] + "# #l";
    }
    cm.sendSimple(selStr);
}

function action(mode, type, selection) {
    if (mode == 1) {
	cm.warp(maps[selection], 0);
    }
    cm.dispose();
}