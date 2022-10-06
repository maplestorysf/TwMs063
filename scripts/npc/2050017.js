var status = -1;

function action(mode, type, selection) {
	if (cm.isQuestActive(3421)) {
		cm.playerMessage("任務完成。");
		cm.forceCompleteQuest(3421);
	}
	cm.dispose();
}
