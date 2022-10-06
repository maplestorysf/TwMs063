var status = -1;

function start(mode, type, selection) {
	status++;
	if (status == 0) {
		qm.sendNext("#b嗯，你最好藉此問一下約翰，那一個藥用的物質是什麼?#k");
	} else {
		qm.gainItem(4032423, 1);
		qm.forceCompleteQuest();
		qm.dispose();
	}
}
function end(mode, type, selection) {
	qm.dispose();
}
