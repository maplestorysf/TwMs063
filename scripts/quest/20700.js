/*
	NPC Name: 		Nineheart
	Description: 		Quest - Are you sure you can leave?
*/

var status = -1;

function start(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        if (status == 1) {
            qm.sendNext("什麼時候你意識到自己是一個弱者，是在要去維多利亞港的時候嗎??");
            qm.safeDispose();
            return;
        }
        status--;
    }
    if (status == 0) {
        qm.sendNext("您終於成為一位騎士了，您確定已經準備接受啟航到 #b維多利亞港#k 了嗎??");
    } else if (status == 1) {
        qm.askAcceptDecline("雖然您已經是一位騎士了，但是您要好好的訓練下去直到變成一位騎士團成員...");
    } else if (status == 2) {
        qm.forceCompleteQuest();
        qm.sendNext("#p1102000#, 去找他將會幫助您成為一個有用的騎士，一旦您到達13等，在那時候我會給你1-2個任務，在這之前請先把自己的水平提升。");
    } else if (status == 3) {
        qm.sendPrev("哦，您知道與 #p1101001#, 她會給你一個祝福嗎??,這個祝福肯定對您有幫助!");
    } else if (status == 4) {
        qm.dispose();
    }
}

function end(mode, type, selection) {
    qm.dispose();
}