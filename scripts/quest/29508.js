var status = -1;

function start(mode, type, selection) {
	qm.dispose();
}
function end(mode, type, selection) {
	if (qm.getPlayer().getMarriageId() > 0 && qm.getPlayer().getGuildId() > 0 && qm.getPlayer().getJunior1() > 0 && qm.canHold(1142081,1)) {
		qm.sendNext("哇！您辦到了厲害！");
		qm.forceCompleteQuest();
		qm.gainItem(1142081,1);
	} else {
		qm.sendNext("我不認為您可以完成此任務。");
	}
	qm.dispose();
}
