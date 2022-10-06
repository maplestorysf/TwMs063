var status = -1;

function start(mode, type, selection) {
	status++;
	if (status == 0) {
		qm.sendNext("嗨！您可以幫我一個忙嗎? #p20000# 這幾天以來他似乎怪怪的....");
	} else if (status == 1) {
		qm.sendNext("他經常抱怨他身體不舒服，但最近他自己覺得全身好了。");
	} else if (status == 2) {
		qm.sendNext("我有一種感覺#p20000#似乎隱藏著什麼不可告人的秘密請幫我偷偷觀察他。");
	} else {
		qm.sendNext("你知道 #p20000# 在那裡是吧? 你往右邊一看就會看到一個低著頭的人 他會帶你去看看約翰。");
		qm.forceStartQuest();
		qm.dispose();
	}
}
function end(mode, type, selection) {
	qm.gainExp(200);
	qm.forceCompleteQuest();
	qm.dispose();
}
