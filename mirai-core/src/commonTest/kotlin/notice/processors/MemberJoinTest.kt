/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

@file:JvmBlockingBridge

package net.mamoe.mirai.internal.notice.processors

import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class MemberJoinTest : AbstractNoticeProcessorTest() {

    @Test
    suspend fun `member actively request join`() {
        suspend fun runTest() = use {
            net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.StructMsg(
                version = 1,
                msgType = 2,
                msgSeq = 16300,
                msgTime = 1630,
                reqUin = 1230001,
                msg = net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsg(
                    subType = 1,
                    msgTitle = "加群申请",
                    msgDescribe = "申请加入 %group_name%",
                    msgAdditional = "verification message",
                    srcId = 1,
                    subSrcId = 5,
                    actions = mutableListOf(
                        net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsgAction(
                            name = "拒绝",
                            result = "已拒绝",
                            actionInfo = net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsgActionInfo(
                                type = 12,
                                groupCode = 2230203,
                            ),
                            detailName = "拒绝",
                        ), net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsgAction(
                            name = "同意",
                            result = "已同意",
                            actionInfo = net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsgActionInfo(
                                type = 11,
                                groupCode = 2230203,
                            ),
                            detailName = "同意",
                        ), net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsgAction(
                            name = "忽略",
                            result = "已忽略",
                            actionInfo = net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.SystemMsgActionInfo(
                                type = 14,
                                groupCode = 2230203,
                            ),
                            detailName = "忽略",
                        )
                    ),
                    groupCode = 2230203,
                    groupMsgType = 1,
                    groupInfo = net.mamoe.mirai.internal.network.protocol.data.proto.Structmsg.GroupInfo(
                        appPrivilegeFlag = 67698880,
                    ),
                    groupFlagext3 = 128,
                    reqUinFaceid = 7425,
                    reqUinNick = "user1",
                    groupName = "testtest",
                    groupExtFlag = 1075905600,
                    actionUinQqNick = "user1",
                    reqUinGender = 1,
                    reqUinAge = 19,
                ),
            )
        }

        setBot(1230003)
            .addGroup(2230203, 1230002, name = "testtest", botPermission = MemberPermission.ADMINISTRATOR).apply {
                addMember(1230002, "user2", MemberPermission.OWNER)
            }

        runTest().run {
            assertEquals(1, size)
            val event = single()
            assertIs<MemberJoinRequestEvent>(event)
            assertEquals(1230001, event.fromId)
            assertEquals(2230203, event.groupId)
            assertEquals("verification message", event.message)
            assertEquals("testtest", event.groupName)
        }
    }

    @Test
    suspend fun `member request accepted by other admin`() {
        suspend fun runTest() = use {
            net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.Msg(
                msgHead = net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.MsgHead(
                    fromUin = 2230203,
                    toUin = 1230003,
                    msgType = 33,
                    msgSeq = 45,
                    msgTime = 16,
                    msgUid = 1441,
                    authUin = 1230001,
                    authNick = "user1",
                    extGroupKeyInfo = net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.ExtGroupKeyInfo(
                        curMaxSeq = 1628,
                        curTime = 1630,
                    ),
                    authSex = 2,
                ),
                contentHead = net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.ContentHead(
                ),
                msgBody = net.mamoe.mirai.internal.network.protocol.data.proto.ImMsgBody.MsgBody(
                    richText = net.mamoe.mirai.internal.network.protocol.data.proto.ImMsgBody.RichText(
                    ),
                    msgContent = "00 22 07 BB 01 00 12 C4 B1 02 00 12 C4 B3 06 B9 DC C0 ED D4 B1 00 30 44 38 32 41 43 32 46 33 30 36 46 44 34 35 30 30 36 38 32 46 36 41 38 32 30 31 38 34 41 42 30 43 43 30 32 43 41 33 33 37 41 31 30 38 43 32 36 36".hexToBytes(),
                ),
            )

        }

        val group = setBot(1230003)
            .addGroup(2230203, 1230002, name = "testtest", botPermission = MemberPermission.ADMINISTRATOR).apply {
                addMember(1230002, "user2", MemberPermission.OWNER)
            }

        assertNull(group.members[1230001])

        runTest().run {
            assertEquals(1, size)
            val event = single()
            assertIs<MemberJoinEvent.Active>(event)
            assertEquals(2230203, event.groupId)
            assertEquals(1230001, event.member.id)
            assertNotNull(group.members[1230001])
        }
    }

    @Test
    fun `member request rejected by other admin`() {
        // There is no corresponding event
    }


    @Test
    suspend fun `member joins directly when group allows anyone`() {
        suspend fun runTest() = use {
            net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.Msg(
                msgHead = net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.MsgHead(
                    fromUin = 2230203,
                    toUin = 1230003,
                    msgType = 33,
                    msgSeq = 45,
                    msgTime = 16,
                    msgUid = 1441,
                    authUin = 1230001,
                    authNick = "user1",
                    extGroupKeyInfo = net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.ExtGroupKeyInfo(
                        curMaxSeq = 1628,
                        curTime = 1630,
                    ),
                    authSex = 2,
                ),
                contentHead = net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm.ContentHead(
                ),
                msgBody = net.mamoe.mirai.internal.network.protocol.data.proto.ImMsgBody.MsgBody(
                    richText = net.mamoe.mirai.internal.network.protocol.data.proto.ImMsgBody.RichText(
                    ),
                    msgContent = "00 22 07 BB 01 00 12 C4 B1 02 00 12 C4 B3 06 B9 DC C0 ED D4 B1 00 30 44 38 32 41 43 32 46 33 30 36 46 44 34 35 30 30 36 38 32 46 36 41 38 32 30 31 38 34 41 42 30 43 43 30 32 43 41 33 33 37 41 31 30 38 43 32 36 36".hexToBytes(),
                ),
            )

        }

        val group = setBot(1230003)
            .addGroup(2230203, 1230002, name = "testtest", botPermission = MemberPermission.ADMINISTRATOR).apply {
                addMember(1230002, "user2", MemberPermission.OWNER)
            }

        assertNull(group.members[1230001])

        runTest().run {
            assertEquals(1, size)
            val event = single()
            assertIs<MemberJoinEvent.Active>(event)
            assertEquals(2230203, event.groupId)
            assertEquals(1230001, event.member.id)
            assertNotNull(group.members[1230001])
        }
    }


}