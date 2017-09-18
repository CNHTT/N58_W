package com.szfp.n58.data;

import android.annotation.SuppressLint;
import android.content.Context;

import com.newpos.app.AppContext;
import com.newpos.mpos.tools.BCDUtils;
import com.newpos.mpos.tools.BaseUtils;
import com.szfp.n58.R;
import com.szfp.n58.entity.ReturnData;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * author：ct on 2017/9/18 10:55
 * email：cnhttt@163.com
 */

public class TransactionData {
    // 卡号
    private final static int cardIdTag = 0x5A;
    // 金额
    private final static int amountTag = 0x9F02;
    // 货币代码
    private final static int currencyTag = 0x5F2A;
    // 商户号
    private final static int merchantTag = 0x9F16;
    // 终端号
    private final static int terIdTag = 0x9F1C;
    // 批次号
    private final static int batchIdTag = 0xFFF1;
    // 流水号
    private final static int serialNoTag = 0x9F41;
    // 日期
    private final static int dateTag = 0x9A;
    // 时间
    private final static int timeTag = 0x9F21;
    // 授权码
    private final static int authCodeTag = 0x89;
    // 系统参考号
    private final static int sysRefNoTag = 0xFFF2;
    // 原交易流水号
    private final static int oldSerialNoIdTag = 0xFFF3;

    private String cardId;
    private String amount;
    private String currency;
    private String merchantId;
    private String terminalId;
    private String batchId;
    private String serialNo;
    private String date;
    private String time;
    private String authCode;
    private String sysRefNo;
    private String oldSerialNo;
    private String tType;

    /**
     * 解析返回的交易信息(TLV编码规则)
     *
     * @param cxt
     * @param tlvData TVL数据
     * @return
     */
    @SuppressLint("DefaultLocale")
   public ReturnData parseTLVData(Context cxt, byte[] tlvData) {
        TransactionData data = new TransactionData();
        int tlvInfosize = tlvData.length;
        int index = 0;
        int tag = 0;
        int len = 0;
        byte[] value = null;

        Map<Integer, byte[]> tagValueMap = new HashMap<Integer, byte[]>();

        while (index < tlvInfosize) {
            // Tag 1~2字节
            tag = (tlvData[index] & 0xFF) & 0x1F;
            if (tag == 0x1F) {
                tag = (tlvData[index] & 0xFF);
                index++;
                tag = (tag << 8) | (tlvData[index] & 0xFF);
            } else {
                tag = tlvData[index] & 0xFF;
            }

            index++;

            /*
             * Length域的编码,最多有四个字节, 如果第一个字节的最高位b8为0, b7~b1的值就是value域的长度. 如果b8为1,
             * b7~b1的值指示了下面有几个子字节. 下面子字节的值就是value域的长度.
             */

            int tmpLen = tlvData[index] & 0xFF;
            // 第一个字节的最高位
            int lenBit8 = (tmpLen & 0x80) >>> 7;
            // 第一个字节的b7~b1
            int lenNum = tmpLen & 0x7F;

            if (lenBit8 == 0) {// lenNum就是value域的长度
                len = lenNum;
            } else if (lenBit8 == 1) {// 如果b8为1, lenNum指示了下面有几个子字节
                // 下面子字节的值就是value域的长度
                if (lenNum == 1) {
                    index++;
                    len = tlvData[index] & 0xFF;
                } else if (lenNum == 2) {
                    index++;
                    int len1 = tlvData[index] & 0xFF;
                    index++;
                    len = (len1 << 8) | (tlvData[index] & 0xFF);
                } else if (lenNum == 3) {
                    index++;
                    int len1 = tlvData[index] & 0xFF;
                    index++;
                    int len2 = tlvData[index] & 0xFF;
                    index++;
                    len = (len1 << 16) | (len2 << 8) | (tlvData[index] & 0xFF);
                }
            }

            index++;

            // Value
            if (len > 0 && (index + len) <= tlvInfosize) {
                value = new byte[len];
                System.arraycopy(tlvData, index, value, 0, len);
                index += len;
                tagValueMap.put(tag, value);
            }
        }

        cardId = tagValueMap.get(cardIdTag) != null ? BaseUtils
                .byteArr2HexStr(tagValueMap.get(cardIdTag))
                .replace("F", "").replace("A", "*") : "";

        amount = tagValueMap.get(amountTag) != null ? BCDUtils.bcd2Str(tagValueMap.get(amountTag))
                : "";

        AppContext.getAppContext().setAmount(tagValueMap.get(amountTag));

        currency = tagValueMap.get(currencyTag) != null ? BCDUtils.bcd2Str(tagValueMap
                .get(currencyTag)) : "";
        merchantId = tagValueMap.get(merchantTag) != null ? new String(tagValueMap.get(merchantTag))
                : "";
        terminalId = tagValueMap.get(terIdTag) != null ? new String(tagValueMap.get(terIdTag)) : "";
        batchId = tagValueMap.get(batchIdTag) != null ? BCDUtils.bcd2Str(tagValueMap
                .get(batchIdTag)) : "";
        serialNo = tagValueMap.get(serialNoTag) != null ? BCDUtils.bcd2Str(tagValueMap
                .get(serialNoTag)) : "";

        AppContext.getAppContext().setSerialNo(tagValueMap.get(serialNoTag));

        date = tagValueMap.get(dateTag) != null ? BaseUtils
                .byteArr2HexStr(tagValueMap.get(dateTag)) : "";
        time = tagValueMap.get(timeTag) != null ? BaseUtils
                .byteArr2HexStr(tagValueMap.get(timeTag)) : "";
        authCode = tagValueMap.get(authCodeTag) != null ? new String(tagValueMap.get(authCodeTag))
                : "";
        sysRefNo = tagValueMap.get(sysRefNoTag) != null ? new String(tagValueMap.get(sysRefNoTag))
                : "";
        oldSerialNo = tagValueMap.get(oldSerialNoIdTag) != null ?  BCDUtils.bcd2Str(tagValueMap
                .get(oldSerialNoIdTag)) : "";


        StringBuffer sb = new StringBuffer();
        if (!"".equals(cardId)) {
            sb.append(cxt.getString(R.string.cardId)).append(cardId).append("\n");
            data.cardId =cardId;
        }
        if (!"".equals(amount)) {
            BigDecimal bd1 = new BigDecimal(Double.toString(Double.parseDouble(amount)));
            BigDecimal bd2 = new BigDecimal(Double.toString(0.01));
            String amountStr = bd1.multiply(bd2).toString();

            if (amountStr.contains(".")) {
                // 小数位数
                int decimalsNum = amountStr.substring(amountStr.indexOf(".") + 1,
                        amountStr.length()).length();
                if (decimalsNum < 2) {
                    amountStr += "0";
                }
                amountStr = amountStr.substring(0, amountStr.indexOf(".") + 3);
            }
            sb.append(String.format(cxt.getString(R.string.amount_), amountStr)).append("\n");
            data.amount =String.format(cxt.getString(R.string.amount_), amountStr);
        }
        if (!"".equals(currency)) {
            sb.append(cxt.getString(R.string.currency)).append(currency).append("\n");
            data.currency =currency;
        }
        if (!"".equals(merchantId)) {
            sb.append(cxt.getString(R.string.merchantId)).append(merchantId).append("\n");
            data.merchantId =merchantId;
        }
        if (!"".equals(terminalId)) {
            sb.append(cxt.getString(R.string.terminalId)).append(terminalId).append("\n");
            data.terminalId =terminalId;
        }
        if (!"".equals(batchId)) {
            sb.append(cxt.getString(R.string.batchId)).append(batchId).append("\n");
            data.batchId =batchId;
        }
        if (!"".equals(serialNo)) {
            sb.append(cxt.getString(R.string.serialNo)).append(serialNo).append("\n");
            data.serialNo =serialNo;
        }
        if (!"".equals(date)) {
            sb.append(cxt.getString(R.string.date)).append(fillWord(date, "/")).append("\n");
            data.date =date;
        }
        if (!"".equals(time)) {
            sb.append(cxt.getString(R.string.time)).append(fillWord(time, ":")).append("\n");
            data.time =time;
        }
        if (!"".equals(authCode)) {
            sb.append(cxt.getString(R.string.authCode)).append(authCode).append("\n");
            data.authCode =authCode;
        }
        if (!"".equals(sysRefNo)) {
            sb.append(cxt.getString(R.string.sysRefNo)).append(sysRefNo).append("\n");
            data.sysRefNo =sysRefNo;
        }
        if (!"".equals(oldSerialNo)) {
            sb.append(cxt.getString(R.string.oldSerialNo)).append(oldSerialNo).append("\n");
            data.oldSerialNo =oldSerialNo;
        }

        ReturnData returnData = new ReturnData();
        returnData .setData(data);
        returnData .setStr(sb.toString());
        return returnData;

    }

    public String Stringinsert(String a,String string,int t){
        return a.substring(0,t)+string+a.substring(t+1,2);
    }

    private String fillWord(String src, String word) {
        int len = src.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i += 2) {
            sb.append(src.substring(i, i + 2));
            sb.append(word);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }


    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getSysRefNo() {
        return sysRefNo;
    }

    public void setSysRefNo(String sysRefNo) {
        this.sysRefNo = sysRefNo;
    }

    public String getOldSerialNo() {
        return oldSerialNo;
    }

    public void setOldSerialNo(String oldSerialNo) {
        this.oldSerialNo = oldSerialNo;
    }

    public String gettType() {
        return tType;
    }

    public void settType(String tType) {
        this.tType = tType;
    }
}
