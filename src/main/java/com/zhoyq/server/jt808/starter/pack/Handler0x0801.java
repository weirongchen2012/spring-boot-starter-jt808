/*
 *  Copyright (c) 2020. 衷于栖 All rights reserved.
 *
 *  版权所有 衷于栖 并保留所有权利 2020。
 *  ============================================================================
 *  这不是一个自由软件！您只能在不用于商业目的的前提下对程序代码进行修改和
 *  使用。不允许对程序代码以任何形式任何目的的再发布。如果项目发布携带作者
 *  认可的特殊 LICENSE 则按照 LICENSE 执行，废除上面内容。请保留原作者信息。
 *  ============================================================================
 *  作者：衷于栖（feedback@zhoyq.com）
 *  博客：https://www.zhoyq.com
 *  创建时间：2020
 *
 */

package com.zhoyq.server.jt808.starter.pack;

import com.zhoyq.server.jt808.starter.core.Jt808Pack;
import com.zhoyq.server.jt808.starter.core.PackHandler;
import com.zhoyq.server.jt808.starter.entity.LocationInfo;
import com.zhoyq.server.jt808.starter.entity.MediaInfo;
import com.zhoyq.server.jt808.starter.helper.Analyzer;
import com.zhoyq.server.jt808.starter.helper.ByteArrHelper;
import com.zhoyq.server.jt808.starter.helper.Jt808Helper;
import com.zhoyq.server.jt808.starter.helper.ResHelper;
import com.zhoyq.server.jt808.starter.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 多媒体数据上传
 * @author zhoyq <a href="mailto:feedback@zhoyq.com">feedback@zhoyq.com</a>
 * @date 2018/7/31
 */
@Slf4j
@Jt808Pack(msgId = 0x0801)
public class Handler0x0801 implements PackHandler {
    @Autowired
    private DataService dataService;
    @Autowired
    private ThreadPoolExecutor tpe;

    @Autowired
    private ByteArrHelper byteArrHelper;
    @Autowired
    private ResHelper resHelper;
    @Autowired
    private Analyzer analyzer;
    @Autowired
    private Jt808Helper jt808Helper;

    @Override
    public byte[] handle( byte[] phoneNum, byte[] streamNum, byte[] msgId, byte[] msgBody) {
        log.info("0801 多媒体数据上传 MediaInfoUpload");

        tpe.execute(() -> {
            String phone = byteArrHelper.toHexString(phoneNum);
            // 多媒体信息, 位置信息 , 多媒体数据包
            byte[] mediaInfoData, locationData, mediaData;
            mediaInfoData = byteArrHelper.subByte(msgBody, 0, 8);
            locationData = byteArrHelper.subByte(msgBody, 8, 36);
            if (phoneNum.length == 10) {
                // 2019 版本存在
                mediaData = byteArrHelper.subByte(msgBody, 36);
            } else {
                // 2013 版本存在 2011版本不存在
                boolean isLocationData = jt808Helper.checkLocationData(locationData);
                if (isLocationData) {
                    mediaData = byteArrHelper.subByte(msgBody, 36);
                } else {
                    locationData = null;
                    mediaData = byteArrHelper.subByte(msgBody, 8);
                }
            }

            // 存储多媒体信息
            MediaInfo mediaInfo = analyzer.analyzeMediaInfo(mediaInfoData);
            dataService.mediaInfo(phone, mediaInfo);

            if(locationData != null){
                // 存储定位数据
                LocationInfo locationInfo = analyzer.analyzeLocation(locationData);
                dataService.terminalLocation(phone, locationInfo);
            }

            // 存储多媒体数据包
            dataService.mediaPackage(phone, mediaData);
        });
        return resHelper.getPlatAnswer(phoneNum,streamNum,msgId,(byte) 0);
    }
}
