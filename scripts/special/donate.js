/* global cm */
var status = -1;
var donatePrice = -1;
var donateType = -1;

function start() {
    cm.sendYesNo("捐贈須知:\r\nblablabla...\r\n是否已閱讀須知並進行捐贈?");
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else if (mode === 0) {
        status--;
    }

    var i = -1;
    if (status <= i++) {
        cm.dispose();
    } else if (status === i++) {
        cm.sendGetNumber("請輸入你需要捐贈的金額\r\n#r※捐贈金額必須在 300 以上 20000 以下#k", 300, 300, 20000);
    } else if (status === i++) {
        if (donatePrice === -1) {
            donatePrice = selection;
        }
        if (donatePrice < 300 || donatePrice > 20000) {
            cm.sendOk("#r※捐贈金額必須在 300 以上 20000 以下#k 閣下輸入的金額不在範圍內,請重新嘗試。");
            cm.dispose();
        } else {
            cm.sendSimple("閣下輸入的捐贈金額為#b" + donatePrice + "#k, 能獲得#b" + parseInt(donatePrice * getCashRate(donatePrice)) + "#k點數\r\n請選擇付款方式\r\n"+
                "#L1#超商代碼(全家/萊爾富/OK超商)#l\r\n" +
                "#L2#ATM虛擬帳號轉帳#l\r\n" +
                "#L3#7-11 ibon代碼#l\r\n"
            );
        }
    } else if (status === i++) {
        if (donateType === -1) {
            donateType = selection;
        }
        if (donateType < 1 || donateType > 3) {
            cm.sendOk("發生未知錯誤。");
        } else {
            //donate fuction
        }
        cm.dispose();
    } else {
        cm.dispose();
    }
}

function getCashRate(price) {
    var cashRate = -1;
    switch (price / 1000) {
        case 1:
            cashRate = 4;
            break;
        case 2:
            cashRate = 4.5;
            break;
        default:
            if (price / 1000 >= 3) {
                cashRate = 5;
            } else {
                cashRate = 3.2;
            }
            break;
    }
    return cashRate;
}