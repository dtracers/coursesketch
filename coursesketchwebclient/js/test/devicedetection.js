﻿function deviceDetection() {

    var mobileIndex = 0;

    if (navigator.userAgent.match(/Android/i))
        mobileIndex = 1;
    else if (navigator.userAgent.match(/BlackBerry/i))
        mobileIndex = 2;
    else if (navigator.userAgent.match(/iPhone|iPad|iPod/i))
        mobileIndex = 3;
    else if (navigator.userAgent.match(/Opera Mini/i))
        mobileIndex = 4;
    else if (navigator.userAgent.match(/webOS/i))
        mobileIndex = 5;
    else if (navigator.userAgent.match(/IEMobile/i))
        mobileIndex = 6;


    switch (mobileIndex) {
        case 0:
            console.log("You are not using a recognized mobile device");
            break;
        case 1:
            console.log("You are using a Android Device");
            break;
        case 2:
            console.log("You are using a Blackberry Device");
            break;
        case 3:
            console.log("You are using a iOS Device");
            break;
        case 4:
            console.log("You are using a Opera Mini Device");
            break;
        case 5:
            console.log("You are using a webOS Device");
            break;
        case 6:
            console.log("You are using a Windows Device");
            break;
    }
}

