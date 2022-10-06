load('nashorn:mozilla_compat.js');
importPackage(Packages.client);

var status = -1;

function start(mode, type, selection) {
	if (mode == -1) {
		qm.dispose();
	} else {
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			qm.sendNext("哈囉！有空嗎？我是幫助新生的冒險家們的教官，我的名字叫做#p2000#。");
		} else if (status == 1) {
			qm.sendNextPrev("你說誰要我做這種事情啊？ 哇哈哈哈~\r\n你的好奇心還真重啊！很好很好~其實是我自願想要做的。");
		} else if (status == 2) {
			qm.sendAcceptDecline("那…我就來開個玩笑吧！咦呀！");
		} else if (status == 3) {
			if (qm.getPlayer().getStat().getHp() >= 50) {
				qm.getPlayer().getStat().setHp(25, qm.getPlayer());
				qm.getPlayer().updateSingleStat(MapleStat.HP, 25);
			}
			if (!qm.haveItem(2010007))
				qm.gainItem(2010007, 1);
			qm.sendNext("嚇到了吧？HP變成0可是件大事呢！來~把#r#t2010007##k送給你，你就吃下它吧！你會充滿活力喔！你就開啟道具視窗，再對要使用的道具點兩下滑鼠左鍵吧！#I");
		} else if (status == 4) {
			qm.sendPrev("記得要我把給你的#t2010007#全部吃掉喔！不過只要靜止不動也可以慢慢的恢復HP…等HP完全恢復後，再跟我說話吧！#I");
		} else if (status == 5) {
			qm.forceStartQuest();
			qm.dispose();
		}
	}
}

function end(mode, type, selection) {
	if (mode == -1) {
		qm.dispose();
	} else {
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			qm.sendNext("使用道具…如何？很簡單吧！在畫面右下方欄位上，還可以設置#b熱鍵#k喔！沒想到吧？ 哈哈~");
		} else if (status == 1) {
			qm.sendNextPrev("很好！看來你已經學會了不少東西…那我就送個禮物給你吧！如果想去世界各地旅行，這可是必須要學會的，所以你應該感激我啊！你可以在危急的時候使用。");
		} else if (status == 2) {
			qm.sendNextPrev("我能教你的也就到此為止了，就算覺得依依不捨，但也還是要道別了！記得要注意自己身體喔！那…再見囉！\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#v2010000# 3 #t2010000#\r\n#v2010009# 3 #t2010009#\r\n\r\n#fUI/UIWindow.img/QuestIcon/8/0# 10 exp");
		} else if (status == 3) {
			if (qm.canHold(2010000) && qm.canHold(2010009)) {
				qm.gainExp(10);
				qm.gainItem(2010000, 3);
				qm.gainItem(2010009, 3);
				qm.forceCompleteQuest();
			} else
				qm.dropMessage(1, "確認一下背包欄位是否已經滿了吧！");
				qm.dispose();
		}
	}
}
