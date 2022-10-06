/*
	NPC Name: 		Nineheart
	Description: 		Quest - Cygnus movie Intro
*/
var status = -1;

function start(mode, type, selection) {
    if (mode == 1)
        status++;
    else
        status--;

    if (status == 0) {
        qm.sendNext("我可以告訴您，您已經努力訓練了，看到您已經達到10等了，我認為這是從貴族翻身的時候了......");
    } else if (status == 1) {
        qm.sendNext("沒有單一的一條路，事實上，有五個種族為您設計的，這取決於您的選擇，如果您決定的話.");
    } else if (status == 2) {
        qm.sendNext("您怎麼看??您有興趣成為騎士團嗎?? 您做好決定再告訴我吧.#b#L0#告訴我如何成為皇家騎士團#l ..#b#L1#不，我沒有關係");
    } else if (status == 3) {
        qm.sendYesNo("您確定嗎??");
        // IF selected no
        //Talk to me after you have decided what you really want to do. Whatever you choose, you will not miss out or lose privileges, so don't take this too seriously...
    } else if (status == 4) {
        qm.forceStartQuest();
        qm.playerSummonHint(false);
        qm.MovieClipIntroUI(true);
        qm.warp(913040100, 0);
        qm.dispose();
    }
}

function end(mode, type, selection) {
    if (mode == 1)
        status++;
    else
        status--;
    if (status == 0) {
        qm.sendNextPrev("測試..");
        qm.dispose();
    }
}