/* Author: Xterminator
	NPC Name: 		Peter
	Map(s): 		Maple Road: Entrance - Mushroom Town Training Camp (3)
	Description: 	Takes you out of Entrace of Mushroom Town Training Camp
*/
var status = -1;

function action(mode, type, selection) {
    if (mode == 1) {
	status++;
    } else {
	status--;
    }
    if (status == 0) {
	cm.sendNext("你完成所有訓練了？做得好！看起來，你已經準備好開始你的冒險之旅了，我將帶你到下一個地方。");
    } else if (status == 1) {
	cm.sendNextPrev("但是請記住，一旦離開這裡，就會遇到很多怪物，加油，再見！");
    } else if (status == 2) {
	cm.warp(40000, 0);
	cm.gainExp(3);
	cm.dispose();
    }
}