package com.ysoftsafeqmobileprintsampleapp.sdk;

import android.print.PrintAttributes;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by barton on 17.7.2015.
 */
public class IppRequest {
    // tags
    private static final byte INTEGER_TAG = 0x21;
    private static final byte BOOLEAN_TAG = 0x22;
    private static final byte NAME_WITHOUT_LANGUAGE_TAG = 0x42;
    private static final byte KEYWORD_TAG = 0x44;
    private static final byte URI_TAG = 0x45;
    private static final byte CHARSET_TAG = 0x47;
    private static final byte NATURAL_LANGUAGE_TAG = 0x48;

    // ipp attributes
    private static final byte[] IPPVERSION = new byte[] {0x01, 0x01};
    private static final byte[] OPERATIONID = new byte[] {0x00, 0x02};
    private static final byte[] REQUESTID = new byte[] {0x00, 0x00, 0x00, 0x01};

    // ipp sections identifiers
    private static final byte OPERATIONATTRIBUTES = 0x01;
    private static final byte JOBATTRIBUTES = 0x02;
    private static final byte ENDATTRIBUTES = 0x03;

    private String label = "print";

    public String printerUri = "";
    public Boolean bwPrint = false;
    public String sides = "one-sided";
    public byte[] printJobData;

    private ByteArrayOutputStream outPutBytes;

    IppRequest(String mlabel)  {
        this.label = mlabel;

        outPutBytes = new ByteArrayOutputStream();
    }

    void generateIppRequest() throws IOException {
        setIppAttributes();

        outPutBytes.write(OPERATIONATTRIBUTES);
        setOperationAttributes();

        if (bwPrint) {
            writeAttribute(NAME_WITHOUT_LANGUAGE_TAG, "print-color-mode", "monochrome");
        } else {
            writeAttribute(NAME_WITHOUT_LANGUAGE_TAG, "print-color-mode", "color");
        }

        writeAttribute(NAME_WITHOUT_LANGUAGE_TAG, "sides", this.sides);

        outPutBytes.write(JOBATTRIBUTES);
        setJobAttributes();

        outPutBytes.write(ENDATTRIBUTES);

        setData(printJobData);
        outPutBytes.flush();
    }

    byte[] getBytes() {
        return outPutBytes.toByteArray();
    }

    private void setData(byte[] printJobData) throws IOException {
        outPutBytes.write(printJobData);
    }

    private void setIppAttributes() throws IOException {
        outPutBytes.write(IPPVERSION);
        outPutBytes.write(OPERATIONID);
        outPutBytes.write(REQUESTID);
    }

    private void setOperationAttributes() throws IOException {
        writeAttribute(CHARSET_TAG, "attributes-charset", "us-ascii");
        writeAttribute(NATURAL_LANGUAGE_TAG, "attributes-natural-language", "en-us");
        if (!this.printerUri.isEmpty()) {
            writeAttribute(URI_TAG, "printer-uri", this.printerUri);
        }
        writeAttribute(NAME_WITHOUT_LANGUAGE_TAG, "job-name", this.label);
        writeAttribute(BOOLEAN_TAG, "ipp-attribute-fidelity", new byte[] {0x01});  // 1 - TRUE
        /*if (printJobInfo.getAttributes().getColorMode() == PrintAttributes.COLOR_MODE_MONOCHROME)
            writeAttribute(NAME_WITHOUT_LANGUAGE_TAG, "output-mode", "monochrome");*/
    }

    private void setJobAttributes() throws IOException {
        writeAttribute(INTEGER_TAG, "copies", new byte[] {0x00, 0x00, 0x00, 0x01});  // 1 copy (number of copies)
        writeAttribute(KEYWORD_TAG, "sides", "one-sided");
    }

    private void writeAttribute(byte tag, String name, String value) throws IOException {
        writeAttribute(tag, name, value.getBytes(StandardCharsets.UTF_8));
    }

    private void writeAttribute(byte tag, String name, byte[] value) throws IOException {
        outPutBytes.write(tag);
        outPutBytes.write(name.length() / 256);
        outPutBytes.write(name.length() % 256);
        outPutBytes.write(name.getBytes(StandardCharsets.UTF_8));
        outPutBytes.write(value.length / 256);
        outPutBytes.write(value.length % 256);
        outPutBytes.write(value);
    }
}