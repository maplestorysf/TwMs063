var status = -1;

function action(mode, type, selection) {
	var em = cm.getEventManager("Romeo");
	if (em == null) {
		cm.sendOk("...");
		cm.dispose();
		return;
	}
	if (em.getProperty("stage").equals("1") && em.getProperty("stage5").equals("0")) {
		//advance to angry!
		cm.sendOk("什麼...這是個可疑的陰謀?這不能...");
		em.setProperty("stage", "2");
	} else if (em.getProperty("stage5").equals("1") && cm.getMap().getAllMonstersThreadsafe().size() == 0) {
		cm.sendOk("繼續..");
		em.setProperty("stage5", "2");
		cm.getMap().setReactorState();
	} else {
		cm.sendOk("...");
	}
	cm.dispose();
}
