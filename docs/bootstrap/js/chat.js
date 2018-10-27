/**
 * Created by yu on 2017/1/15.
 */
var socket = null;
var isAuth = false;
var userNick = null;
var userCount = 0;
var ws_url="ws://localhost:8000/websocket";
$(function () {
    $("#menuModal").modal('show');
    var height = $(window).height();
    $('#content').css("height", height - $('#top').height() - $('#opt').height() - 40);

    $('#loginBtn').click(function () {
        userLogin();
    });

    $('#faceBtn').qqFace({
        id: 'facebox',
        assign: 'mess',
        path: 'arclist/'	//表情存放的路径
    });

    $('#sendBtn').click(function () {
        var mess = $("#mess").val().trim();
        if (mess) {
            sendMess(mess);
            $("#mess").val('');
        }
    }).keyup(function (e) {
        var keyCode = e.which || e.keyCode;
        if (keyCode == 13) {
            $("#sendBtn").click();
        }
    });
});

function sendMess(mess) {
    send(true, "{'code':10086,'mess':'" + mess + "'}");
};


function userLogin()
{
    if (!userNick)
    {
        userNick = $('#nick').val().trim();
    }

    if (userNick)
    {
        if (!window.WebSocket)
        {
            window.WebSocket = window.MozWebSocket;
        }

        if (window.WebSocket)
        {

            window.socket = new WebSocket(ws_url);

            window.socket.onopen = function (event)// shake hands, build connect
            {
                console.log("connection success!!");
                sendAuthInfo();
            };

            window.socket.onmessage = function (event)
            {
                var data = JSON.parse(event.data);
                console.log(data);
                console.log("onmessage data: " + JSON.stringify(data));
                switch (data.type) {
                    case 10015: // ping message
                        console.log("ping message: " + JSON.stringify(data));
                        pingInvoke(data);
                        break;
                    case 988: // system message
                        console.log("system message: " + JSON.stringify(data));
                        sysInvoke(data);
                        break;
                    case 1244: // error message
                        console.log("error message: " + JSON.stringify(data));
                        closeInvoke(null);
                        break;
                    case 5 << 8 | 220: // auth message
                    console.log("auth message: " + JSON.stringify(data));
                    break;
                    case 10086: // broadcast message
                        console.log("broadcast message: " + JSON.stringify(data));
                        broadcastInvoke(data);
                        break;

                }
            };

            window.socket.onclose = function (event) {
                console.log("connection close!!!");
                closeInvoke(event);
            };


        }
        else
        {
            alert("您的浏览器不支持WebSocket！！！");
        }
    }
    else
    {
        $('#tipMsg').text("请输入昵称");
        $('#tipModal').modal('show');
    }
}

function sendAuthInfo()
{
    var obj = {};
    obj.code = 10000;
    obj.nick = $('#nick').val().trim();
    send(true, JSON.stringify(obj));
};

function send(auth, mess)
{
    if (!window.socket)
    {
        return;
    }
    if (socket.readyState == WebSocket.OPEN || auth) {
        console.log("send: " + mess);
        window.socket.send(mess);
    }
    else
    {
        $('#tipMsg').text("连接没有成功，请重新登录");
        $('#tipModal').modal('show');
    }
}
;


function closeInvoke(event) {
    window.socket = null;
    window.isAuth = false;
    window.userCount = 0;
    $('#tipMsg').text("登录失败，网络连接异常");
    $('#tipModal').modal('show');
}
;

/**
 * 处理系统消息
 * @param data
 */
function sysInvoke(data)
{
    switch (data.extend.code)
    {
        case 20001: // user count
            console.log("current user: " + data.extend.mess);
            userCount = data.extend.mess;
            $('#userCount').text(userCount);
            break;
        case 20002: // auth
            console.log("auth result: " + data.extend.mess);
            isAuth = data.extend.mess;
            if (isAuth)
            {
                $("#menuModal").modal('hide');
                $('#chatWin').show();
                $('#content').append('欢迎来到嗨皮聊天室！！');
                // $('#content').scrollTop($('#content')[0].scrollHeight);
            }
            break;
        case 20003: // system other message
            console.log("system other message: " + data.extend.mess);
            break;
    }
};

/**
 * 处理广播消息
 * @param data
 */
function broadcastInvoke(data) {
    var mess = data.body;
    var nick = data.extend.nick;
    var uid = data.extend.uid;
    var time = data.extend.time;
    mess = replace_em(mess);
    var html = '<div class="title">' + nick + '&nbsp;(' + uid + ') &nbsp;' + time + '</div><div class="item">' + mess + '</div>';
    $("#content").append(html);
    $('#content').scrollTop($('#content')[0].scrollHeight);

}
;

function erorInvoke(data) {

};

function pingInvoke(data) {
    //发送pong消息响应
    send(isAuth, "{'code':10016}");
};

//查看结果
function replace_em(str) {
    str = str.replace(/\</g, '&lt;');
    str = str.replace(/\>/g, '&gt;');
    str = str.replace(/\n/g, '<br/>');
    str = str.replace(/\[em_([0-9]*)\]/g, '<img src="arclist/$1.gif" border="0" />');
    return str;
};