/*
修改by宗達 20160403 12:48
*/

var Message = new Array(
    "如果遇到不能點技能/能力值/不能進傳點/不能點NPC,請在對話框打@ea就可以了",
    "/找人 玩家名字 可以用來找人喔",
	"禁止開外掛，遊戲愉快！！",
	"關於伺服器指令可以使用@help/@幫助查看",
	"有卡商店或不能領商店物品請打@jk_hm",
    "如有bug請回報GM",
	"@在線點數/@jcds 可以領取在線點數唷");

var setupTask;

function init() {
    scheduleNew();
}

function scheduleNew() {
    setupTask = em.schedule("start", 300000);
}

function cancelSchedule() {
    setupTask.cancel(true);
}

function start() {
    scheduleNew();
    em.broadcastYellowMsg("[辛巴谷 公告]" + Message[Math.floor(Math.random() * Message.length)]);
}

