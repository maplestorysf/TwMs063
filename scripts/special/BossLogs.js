var status = -1;

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	var pap = cm.getBossLog("pop");
	var horntail = cm.getBossLog("龍王次數");
	var lionbear = cm.getBossLog("熊獅王次數");
	if (mode == 1) {
		status++;
	} else {
		status--;
	}
	if (status == 0) {
		cm.sendSimple("親愛的 #h \r\n 您好我是#p9209006#\r\n以下是您今天各種東西挑戰次數\r\n#r請注意!!!\r\n每日挑戰次數是以今天完成兩次開始算起的24小時#k \r\n熊獅王挑戰次數: "+lionbear+"/2\r\n闇黑龍王挑戰次數 "+horntail+"/2\r\n拉圖斯挑戰次數: "+pap+"/2\r\n");
	}
	cm.dispose();
}