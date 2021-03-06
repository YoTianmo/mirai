/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress("EXPERIMENTAL_API_USAGE")

package net.mamoe.mirai.qqandroid.network.protocol.packet.chat

import kotlinx.io.core.ByteReadPacket
import net.mamoe.mirai.qqandroid.QQAndroidBot
import net.mamoe.mirai.qqandroid.network.Packet
import net.mamoe.mirai.qqandroid.network.QQAndroidClient
import net.mamoe.mirai.qqandroid.network.protocol.data.proto.MsgRevokeUserDef
import net.mamoe.mirai.qqandroid.network.protocol.data.proto.MsgSvc
import net.mamoe.mirai.qqandroid.network.protocol.packet.OutgoingPacket
import net.mamoe.mirai.qqandroid.network.protocol.packet.OutgoingPacketFactory
import net.mamoe.mirai.qqandroid.network.protocol.packet.buildOutgoingUniPacket
import net.mamoe.mirai.qqandroid.utils._miraiContentToString
import net.mamoe.mirai.qqandroid.utils.io.serialization.readProtoBuf
import net.mamoe.mirai.qqandroid.utils.io.serialization.toByteArray
import net.mamoe.mirai.qqandroid.utils.io.serialization.writeProtoBuf

internal class PbMessageSvc {
    object PbMsgWithDraw : OutgoingPacketFactory<PbMsgWithDraw.Response>(
        "PbMessageSvc.PbMsgWithDraw"
    ) {
        sealed class Response : Packet {
            object Success : Response() {
                override fun toString(): String {
                    return "PbMessageSvc.PbMsgWithDraw.Response.Success"
                }
            }

            data class Failed(
                val result: Int,
                val errorMessage: String
            ) : Response()
        }

        // 12 1A 08 01 10 00 18 E7 C1 AD B8 02 22 0A 08 BF BA 03 10 BF 81 CB B7 03 2A 02 08 00
        fun createForGroupMessage(
            client: QQAndroidClient,
            groupCode: Long,
            messageSequenceId: Int, // 56639
            messageRandom: Int, // 921878719
            messageType: Int = 0
        ): OutgoingPacket = buildOutgoingUniPacket(client) {
            writeProtoBuf(
                MsgSvc.PbMsgWithDrawReq.serializer(),
                MsgSvc.PbMsgWithDrawReq(
                    groupWithDraw = listOf(
                        MsgSvc.PbGroupMsgWithDrawReq(
                            subCmd = 1,
                            groupType = 0, // 普通群
                            groupCode = groupCode,
                            msgList = listOf(
                                MsgSvc.PbGroupMsgWithDrawReq.MessageInfo(
                                    msgSeq = messageSequenceId,
                                    msgRandom = messageRandom,
                                    msgType = messageType
                                )
                            ),
                            userdef = MsgRevokeUserDef.MsgInfoUserDef(
                                longMessageFlag = 0
                            ).toByteArray(MsgRevokeUserDef.MsgInfoUserDef.serializer())
                        )
                    )
                )
            )
        }

        fun createForTempMessage(
            client: QQAndroidClient,
            groupUin: Long,
            toUin: Long,
            messageSequenceId: Int, // 56639
            messageRandom: Int, // 921878719
            time: Int
        ): OutgoingPacket = buildOutgoingUniPacket(client) {
            writeProtoBuf(
                MsgSvc.PbMsgWithDrawReq.serializer(),
                MsgSvc.PbMsgWithDrawReq(
                    c2cWithDraw = listOf(
                        MsgSvc.PbC2CMsgWithDrawReq(
                            subCmd = 1,
                            msgInfo = listOf(
                                MsgSvc.PbC2CMsgWithDrawReq.MsgInfo(
                                    fromUin = client.bot.id,
                                    toUin = toUin,
                                    msgSeq = messageSequenceId,
                                    msgUid = 1000000000000000000L or messageRandom.toULong().toLong(),
                                    msgTime = time.toLong(),
                                    routingHead = MsgSvc.RoutingHead(
                                        grpTmp = MsgSvc.GrpTmp(groupUin, toUin)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        fun createForFriendMessage(
            client: QQAndroidClient,
            toUin: Long,
            messageSequenceId: Int, // 56639
            messageRandom: Int, // 921878719
            time: Int
        ): OutgoingPacket = buildOutgoingUniPacket(client) {
            writeProtoBuf(
                MsgSvc.PbMsgWithDrawReq.serializer(),
                MsgSvc.PbMsgWithDrawReq(
                    c2cWithDraw = listOf(
                        MsgSvc.PbC2CMsgWithDrawReq(
                            subCmd = 1,
                            msgInfo = listOf(
                                MsgSvc.PbC2CMsgWithDrawReq.MsgInfo(
                                    fromUin = client.bot.id,
                                    toUin = toUin,
                                    msgSeq = messageSequenceId,
                                    msgUid = 1000000000000000000L or messageRandom.toULong().toLong(),
                                    msgTime = time.toLong(),
                                    routingHead = MsgSvc.RoutingHead(
                                        c2c = MsgSvc.C2C(
                                            toUin = toUin
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        override suspend fun ByteReadPacket.decode(bot: QQAndroidBot): Response {
            val resp = readProtoBuf(MsgSvc.PbMsgWithDrawResp.serializer())
            resp.groupWithDraw?.firstOrNull()?.let {
                if (it.result != 0) {
                    return Response.Failed(it.result, it.errmsg)
                }
                return Response.Success
            }
            resp.c2cWithDraw?.firstOrNull()?.let {
                if (it.result != 0) {
                    return Response.Failed(it.result, it.errmsg)
                }
                return Response.Success
            }
            return Response.Failed(-1, "No response")
        }
    }
}