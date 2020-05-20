package com.ysoftsafeqmobileprintsampleapp.sdk;

/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okio.Buffer;

public final class CustomTrust {

    private OkHttpClient.Builder clientBuilder;

    public OkHttpClient.Builder getClientBuilder() {
        return this.clientBuilder;
    }

    public CustomTrust() {
        X509TrustManager trustManager;
        X509TrustManager defaultTrustManager;
        X509TrustManager trustAllCerts =
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                };

        SSLSocketFactory sslSocketFactory;
        try {
            defaultTrustManager = createTrustManager(null);
            trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllCerts}, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        this.clientBuilder = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts);
    }

    private X509TrustManager createTrustManager(KeyStore store) throws NoSuchAlgorithmException, KeyStoreException {
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init((KeyStore) store);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        return (X509TrustManager) trustManagers[0];
    }

    /**
     * Returns an input stream containing one or more certificate PEM files. This implementation just
     * embeds the PEM files in Java strings; most applications will instead read this from a resource
     * file that gets bundled with the application.
     */
    private InputStream trustedCertificatesInputStream() {
        // PEM files for root certificates of Comodo and Entrust. These two CAs are sufficient to view
        // https://publicobject.com (Comodo) and https://squareup.com (Entrust). But they aren't
        // sufficient to connect to most HTTPS sites including https://godaddy.com and https://visa.com.
        // Typically developers will need to get a PEM file from their organization's TLS administrator.
        String supice = "-----BEGIN CERTIFICATE-----\n" +
                "MIIEjjCCA3agAwIBAgIQJ9zhpuh03aRARf7SpWwl1TANBgkqhkiG9w0BAQsFADA/\n" +
                "MRUwEwYKCZImiZPyLGQBGRYFbG9jYWwxFTATBgoJkiaJk/IsZAEZFgV5c29mdDEP\n" +
                "MA0GA1UEAxMGc3VwaWNlMB4XDTA3MDgxNjE1MTc1MloXDTIyMDExODEyMzczNVow\n" +
                "PzEVMBMGCgmSJomT8ixkARkWBWxvY2FsMRUwEwYKCZImiZPyLGQBGRYFeXNvZnQx\n" +
                "DzANBgNVBAMTBnN1cGljZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                "AKHPSCi+wJVRO/gwAKfuhA7Fdn3Q++84Xn0ao0Z3CbtLvFPYMnNzkjwrjoBYmW+6\n" +
                "SLAgHU37keF1sk/0RCWJysVpn/zL7zvH9tzHabrBh4Dccrh0rfVXX21HR8o2Cseg\n" +
                "8OAfCmX5Nv2ET1H2GiQgzscYvXtYwnC8NzpO3w6PwpZTfAtqHGMCF3pW3KKf3pTj\n" +
                "1NoNOeJJwBPN1aCCQzH9GpUSk5bSJbRTKA+bnm6HFOtDS9qt0ZD/3UTw33EjXIYK\n" +
                "ltLuqGihMRUvPvBvOC2EOWr9oABe9o10SKMtOxwHnBtZeqZSCnENoNk1TPxc62An\n" +
                "P7/5cEI23BgfcHRCS37lKXECAwEAAaOCAYQwggGAMBMGCSsGAQQBgjcUAgQGHgQA\n" +
                "QwBBMAsGA1UdDwQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBS+SNCc\n" +
                "3Ak76HD9oTEageu6DFN0JjCB9AYDVR0fBIHsMIHpMIHmoIHjoIHghoGsbGRhcDov\n" +
                "Ly9DTj1zdXBpY2UsQ049c3VwaWNlLENOPUNEUCxDTj1QdWJsaWMlMjBLZXklMjBT\n" +
                "ZXJ2aWNlcyxDTj1TZXJ2aWNlcyxDTj1Db25maWd1cmF0aW9uLERDPXlzb2Z0LERD\n" +
                "PWxvY2FsP2NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3Q/YmFzZT9vYmplY3RDbGFz\n" +
                "cz1jUkxEaXN0cmlidXRpb25Qb2ludIYvaHR0cDovL3N1cGljZS55c29mdC5sb2Nh\n" +
                "bC9DZXJ0RW5yb2xsL3N1cGljZS5jcmwwEAYJKwYBBAGCNxUBBAMCAQIwIwYJKwYB\n" +
                "BAGCNxUCBBYEFN9MKfLaRQn8Js6cYTJjkaweGaXSMA0GCSqGSIb3DQEBCwUAA4IB\n" +
                "AQCZt3BGnI3lhFbSlPwIj4VHZhqk0HtlzVnrctjv6zjjKlQSZT4iuCIOH3SOyCDz\n" +
                "20DM1CUIeZOhd+EBNYXW9hzzObB4jY7kxGaCnsq5KcltF9K60PKV9pfIlvMb5P++\n" +
                "4PeoyNfkZ0kv0gt94/dmnA2v9cHKnpavrvvMa8AHDv6e5FckyEY6NmhqeHJNcpUc\n" +
                "rTBbbD7WJUFfGsYTE+d2F0rSHedXN3cbFaFLTHVsV2iCNfCjv7qlns1uaAeX/fSq\n" +
                "4Z60iFns3xWJnIcuTDkmjOxa6JJLHG04WOLuFdPw4MCakLMNUoY58Cjla8wXzC7Z\n" +
                "E9ZPtcb9J1Vx/4aENOGpMeRS\n" +
                "-----END CERTIFICATE-----\n";
        String rndca = "-----BEGIN CERTIFICATE-----\n" +
                "MIIG5jCCBM6gAwIBAgIBCDANBgkqhkiG9w0BAQ0FADCBizELMAkGA1UEBhMCY3ox\n" +
                "DTALBgNVBAcTBEJybm8xGzAZBgNVBAoTElkgU29mdCBDb3Jwb3JhdGlvbjEMMAoG\n" +
                "A1UECxMDUm5EMRUwEwYDVQQDEwxZU29mdCBSbkQgQ0ExHTAbBgkqhkiG9w0BCQEW\n" +
                "DmluZm9AeXNvZnQuY29tMQwwCgYDVQQEEwNEWkEwIBcNMTQwMjA0MTMzMDIxWhgP\n" +
                "MjA5OTEyMTcxNDE1MTdaMIGmMQswCQYDVQQGEwJjejENMAsGA1UEBxMEQnJubzEb\n" +
                "MBkGA1UEChMSWSBTb2Z0IENvcnBvcmF0aW9uMQwwCgYDVQQLEwNSbkQxMDAuBgNV\n" +
                "BAMTJ1lTb2Z0IHBheW1lbnQgc3lzdGVtIHNlcnZlciBjZXJ0aWZpY2F0ZTEdMBsG\n" +
                "CSqGSIb3DQEJARYOaW5mb0B5c29mdC5jb20xDDAKBgNVBAQTA2R6YTCCAiIwDQYJ\n" +
                "KoZIhvcNAQEBBQADggIPADCCAgoCggIBAKeedoSGKfRZbGbn4ItQPSRXRVF3p9m9\n" +
                "6TKiJAc2zLZmx/K0kVfdtNuDr9nEPTrWHQrBYTGdQHrXLjKbt8zZ5QVeQRRLw9ok\n" +
                "mQFo5vJOWADeN8keJUuLluad0s+9LKEh35U/r3fHOCiafJZzDR9bPGNhRnWYC8+F\n" +
                "L06SNMcJPOpSlbc0Oxccq7m5qewxL1GaeRbA8lGioiQhgZqEIfE4ZLIBoNoOTI+L\n" +
                "ApBEuMSPpqF4k22qjV/D5MMmigcA9XMJCwLGxZE4zMBGvxRWDvPxvZ9ZBaAH0/bM\n" +
                "WfCGrA83L1Gn9WIfdQIuboKKg8en0P44mXzO0Q3qy8hCbEeIKrxnMPohYnSdQL3h\n" +
                "5DpnUHqJTGA3UqmVg95iiFIBnBHC0F57lab5iQ5H59ZB4KD9dfbYrrphGSOs9Mbc\n" +
                "QHwdDgVkQLPKxv71t79brc5xMymsKX+7YL3+sC+BTvVlvmG4CHrEK7+HiOK7yKoK\n" +
                "u+H3m2tXP+TbVaQ7Xq4F2KQ4p9G/xA+bs/uXJyRR2Z9ouKDOv05Dgm8Owt2/yHSG\n" +
                "+dLNRd6Xz+L4DnZtiOe9jIq+7phn3eICuUyyrMa6+gDE6YJyDaaPrzc/36RB8t2V\n" +
                "+I9MwDccWaAcWyqSpjqKRwyY40Rv7Buvl5hDgobYf35AhDkv4Vu64vWcJbaVwbiZ\n" +
                "uEmPJq9tEXCbAgMBAAGjggE0MIIBMDAPBgNVHRMBAf8EBTADAgEAMB0GA1UdDgQW\n" +
                "BBQHB9H1AkI6hHA7isu+ZpBYztJzITCBvQYDVR0jBIG1MIGygBRsX+zIeY4vA4v5\n" +
                "6DnduOWJYQ1cOaGBlqSBkzCBkDELMAkGA1UEBhMCY3oxDTALBgNVBAcTBEJybm8x\n" +
                "GzAZBgNVBAoTElkgU29mdCBDb3Jwb3JhdGlvbjEMMAoGA1UECxMDUm5EMRowGAYD\n" +
                "VQQDExFZU29mdCBSbkQgcm9vdCBDQTEdMBsGCSqGSIb3DQEJARYOaW5mb0B5c29m\n" +
                "dC5jb20xDDAKBgNVBAQTA0RaQYIBAjALBgNVHQ8EBAMCBeAwEQYJYIZIAYb4QgEB\n" +
                "BAQDAgZAMB4GCWCGSAGG+EIBDQQRFg94Y2EgY2VydGlmaWNhdGUwDQYJKoZIhvcN\n" +
                "AQENBQADggIBACVh5BXX4d706MHWLfo9fpe7NlJYhSwU5r6kI0uXvn7gyGvq86x2\n" +
                "7qINQwAXHeGJBLGqmEpbGuiPND+IT461/gkaaSUOyaZ1/AfVk7Eek3E7Vl7gzHsl\n" +
                "eNM3vdmEIogB7+CO30Ud0P6VlibKXuye94E47arAKT2f+lbxlZX5+vj4Tqptm9lM\n" +
                "I9JhphP63pouJMGXkb/DcWwWWT5T6eftJ21LHqCKhsb2N6Kb1hGT5OaiX/suRy01\n" +
                "o5FF7wt6VPPc7fTwtdo/BiaDECqH9uyXe7oZ7LJ2NbhMio3szOrCRwra7P8GZLKc\n" +
                "bfIFNqe99/CTbeW9EQ3kJdhycNIsu2p1Z1fa/6Fj3/p+SQTk452/6TCYHV9l9xgN\n" +
                "RrsWZPamDqv4jT1rZx8PgCeQJ0h6KSrM2J2nhf1EWhfGrHR7pO32+WwHsOIO7rrL\n" +
                "4TS7e9eizPfMuDQs4xx8g4Ju58LoWoeyMlCHR6mk3MSaYHDoQaHYpw0cRpO1kqrG\n" +
                "6ezXnwYj0TRdLcHpAz4pvoORhaw8KbJKQwt/eeM+bC0PRaT27Q60V4OyJAgtWzV/\n" +
                "hxuc3l4ay1qKM9eRwjkc8CvKQKFcZV9WrVkkX2dL6gkTDJbk7XDT16jY65GzNTk8\n" +
                "WF+pMZsL/++HRZhjshPGVLmEQjlnXrVf7AAmWeQRkMEFB77I6m0vI21b\n" +
                "-----END CERTIFICATE-----\n";
        String rndca2 = "-----BEGIN CERTIFICATE-----\n" +
                "MIIG0zCCBLugAwIBAgIBAjANBgkqhkiG9w0BAQ0FADCBkDELMAkGA1UEBhMCY3ox\n" +
                "DTALBgNVBAcTBEJybm8xGzAZBgNVBAoTElkgU29mdCBDb3Jwb3JhdGlvbjEMMAoG\n" +
                "A1UECxMDUm5EMRowGAYDVQQDExFZU29mdCBSbkQgcm9vdCBDQTEdMBsGCSqGSIb3\n" +
                "DQEJARYOaW5mb0B5c29mdC5jb20xDDAKBgNVBAQTA0RaQTAgFw0xMzEyMTkxNDE2\n" +
                "NDVaGA8yMDk5MTIxNzE0MTUxN1owgYsxCzAJBgNVBAYTAmN6MQ0wCwYDVQQHEwRC\n" +
                "cm5vMRswGQYDVQQKExJZIFNvZnQgQ29ycG9yYXRpb24xDDAKBgNVBAsTA1JuRDEV\n" +
                "MBMGA1UEAxMMWVNvZnQgUm5EIENBMR0wGwYJKoZIhvcNAQkBFg5pbmZvQHlzb2Z0\n" +
                "LmNvbTEMMAoGA1UEBBMDRFpBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC\n" +
                "AgEAq8BtILfntIjYRcUVIy1MwkVB92OtT22Jp/MDeX/tF7YycM+3MZ6AGNRHZ+6+\n" +
                "6psNKlicbmmpGoCNNfoBjNjDIAwElH4Zzvytb5FxYh8/j5MqwW2C2TMbdRAMJOgS\n" +
                "9P9Ek79yVNvqjJY4u5m8Wi4gmB05VkvT0d99Uxij3TO5BVOWUbZtBhMftTH5jXy6\n" +
                "4zRDsOQ1ay5OXvSanx91OnK91dqAqnF8WUfyEMPwX/Db72ptgmYKuWVmChdg1c8Z\n" +
                "D0/rXLMXmwv79jxEHkZc872PqdOK9rpxoT6VWxj0q0AduP9HR8FqqvyVo9SPD6kR\n" +
                "PvfC6DC4Jf09e84II/kY4FlS1kuM+QWtV4F7+PYdO/6gwenmCzIxZzpLFkvfRdfI\n" +
                "wqJLyiuGhCb9BOrod63LKsVkjiQ8GGihjB/mArw4CFY8AGY6r7mwhCFxecbsUZW9\n" +
                "Aa26Vpgd5k52mahbHvTPLkHlQhD1kG+NQffQ9OCLam4uG9p0EHLmKNDyWhOJfL8C\n" +
                "5gjG4h2JCftJbZWDkX47ALkAdeIFafd1t10f93fA3eq/pSA7ENwUMB3jARbMs874\n" +
                "1XFQVdo3fVqFQBFUzndVPUnLB+CGyxDcpsSjqY5njzjmzQc+a9qR0Gl93BeElJ6A\n" +
                "ltYEjFL/iBlSRnPT94IbJjUifPcf6bzFbPGQWN9Ow7ZOjekCAwEAAaOCATcwggEz\n" +
                "MBIGA1UdEwEB/wQIMAYBAf8CAQEwHQYDVR0OBBYEFGxf7Mh5ji8Di/noOd245Ylh\n" +
                "DVw5MIG9BgNVHSMEgbUwgbKAFCdB3zHbLUTpzj7wSJmz7PJP+XokoYGWpIGTMIGQ\n" +
                "MQswCQYDVQQGEwJjejENMAsGA1UEBxMEQnJubzEbMBkGA1UEChMSWSBTb2Z0IENv\n" +
                "cnBvcmF0aW9uMQwwCgYDVQQLEwNSbkQxGjAYBgNVBAMTEVlTb2Z0IFJuRCByb290\n" +
                "IENBMR0wGwYJKoZIhvcNAQkBFg5pbmZvQHlzb2Z0LmNvbTEMMAoGA1UEBBMDRFpB\n" +
                "ggEBMAsGA1UdDwQEAwIBBjARBglghkgBhvhCAQEEBAMCAAcwHgYJYIZIAYb4QgEN\n" +
                "BBEWD3hjYSBjZXJ0aWZpY2F0ZTANBgkqhkiG9w0BAQ0FAAOCAgEAiFBTlOzYMYWv\n" +
                "JG3rW0q+tTBAQGMGySbeV7PxANcpMWZZrT/5WrKQMPObhxj9yHiGJp2xovmKXoUJ\n" +
                "SImsoh4DZGdkpKrzdPZNwkIAxd+0z75RU9N6/0qu4gZ8wenWlVXBKLn/3Wp19L3E\n" +
                "sebXvlx0ZoMm0MdC9CAtalxMO1dbQPDT1CjQ2NxjYjhRx52DpQJYjbFqjEkCEG50\n" +
                "9i1xNPkG/D+cZycRxzorKi7ZHUGPSwem5LxYLk3AKDuGXMNYoTiX+v29RVolQhZo\n" +
                "ibBIssDQiUvnUnvS++gLvBF5wlmA9nLvCGzFbfwEfrUCQXflP5DCoZDL7We9Wcy+\n" +
                "NapPFrJJ6zcdVg3UkzYD+88i59jB05VwdbeorDyxoZkbmebCpaa4bQ5ImjRZgwqA\n" +
                "+es4twWKlsujXpQfyCxJd4DbD251UBJYaI5kRlQq8CYGhIiAgSVWHyTMdBV4ixIf\n" +
                "sLRZ4zc3FXmeQBdy73+OfcBbQvp3MIXIeyEXQ9DHB+SpJGiy0JxV1SHcwFitQT23\n" +
                "9wShEq9qXAb7D5f/s2MWN1+csvhRdJPii0g7fMgWvyrF1sQGfWy9ZMdE/wEL3LTP\n" +
                "yc7vL3XlCSXlu1N9jmipGTSKvV4Xxf9xuGeXNZwR6zY5EwcrOsOaZnkF+DhY7TcJ\n" +
                "NO7mvov6ujFKbAECGYKw3e37PYUoOZo=\n" +
                "-----END CERTIFICATE-----\n";
        String rndca3 = "-----BEGIN CERTIFICATE-----\n" +
                "MIIGNzCCBB+gAwIBAgIBATANBgkqhkiG9w0BAQ0FADCBkDELMAkGA1UEBhMCY3ox\n" +
                "DTALBgNVBAcTBEJybm8xGzAZBgNVBAoTElkgU29mdCBDb3Jwb3JhdGlvbjEMMAoG\n" +
                "A1UECxMDUm5EMRowGAYDVQQDExFZU29mdCBSbkQgcm9vdCBDQTEdMBsGCSqGSIb3\n" +
                "DQEJARYOaW5mb0B5c29mdC5jb20xDDAKBgNVBAQTA0RaQTAgFw0xMzEyMTkxNDE1\n" +
                "MTdaGA8yMDk5MTIxNzE0MTUxN1owgZAxCzAJBgNVBAYTAmN6MQ0wCwYDVQQHEwRC\n" +
                "cm5vMRswGQYDVQQKExJZIFNvZnQgQ29ycG9yYXRpb24xDDAKBgNVBAsTA1JuRDEa\n" +
                "MBgGA1UEAxMRWVNvZnQgUm5EIHJvb3QgQ0ExHTAbBgkqhkiG9w0BCQEWDmluZm9A\n" +
                "eXNvZnQuY29tMQwwCgYDVQQEEwNEWkEwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
                "ggIKAoICAQC6n3LNV4vI5Kdobz2dlwXPIwk6Vhk45p6lGHfbSuN0rxaMIaBiseL8\n" +
                "jdIYBv6xOQT6O58/hbCa+/Et9o+cxG3m1HQ/DwgLK6OWWL3CPepkmRU/GgZQCZQ1\n" +
                "619r0d534G1rRQIXWRhC3o5H4oVsk3vKz3oQrFPuzAbiLiKAIi9k9qvgzOKSt5YQ\n" +
                "V5pNCOygFV/A7YjMNXcnpekDxM1GObTh9/CNaErC8xP5ZthDhucnGDHXoqStamWm\n" +
                "UICAO8d1jgoaDraxDwHZcDtmT4kXKjtOvpfIyVL/1+FSxE7qauKwtJ/e2IyX7our\n" +
                "gf2KhDGJawmF3gLZCj1MuRd5Zg+GBK61MTzyWnT+0J+HKEixFO8mqrgCdV7BmC2o\n" +
                "JEyyJHeRpIcOUPJYDRorXrDmwzQB4hkLDiACnWQTI1Sc2Tkr7d0ZkuKipgf5YkGC\n" +
                "ZeG/c2geQ++04SxIu9Grb0SOh+LBDhvrQp70rgy1F+NzVjcqVW/vqyP32/M/AJO6\n" +
                "+wuSZVMRNEzEdVEinPvm6WMgZhw0KH5O1KEFJ1r772m/7mUMiOaukRps9NenmUvN\n" +
                "q8YbO8ZfDFi0KrHPR+mLifczxIAie+xIJu0HJKTm0OoSz+rgxRQYaS3RsLIOJePt\n" +
                "gcEIQlc5T9kUfvnVpFuBRxEAaQWhgApge4ALv7O2aF2l0focnkfM1QIDAQABo4GX\n" +
                "MIGUMBIGA1UdEwEB/wQIMAYBAf8CAQIwHQYDVR0OBBYEFCdB3zHbLUTpzj7wSJmz\n" +
                "7PJP+XokMB8GA1UdIwQYMBaAFCdB3zHbLUTpzj7wSJmz7PJP+XokMAsGA1UdDwQE\n" +
                "AwIBBjARBglghkgBhvhCAQEEBAMCAAcwHgYJYIZIAYb4QgENBBEWD3hjYSBjZXJ0\n" +
                "aWZpY2F0ZTANBgkqhkiG9w0BAQ0FAAOCAgEAfmmSihhS+mQkeUuVLodPRy4T9iZ+\n" +
                "WTeVHqcHx4JlYWceqzSLXfaskyCsPIk9ZoShgmNhHnB8VEwGsjYdHpP9/MWG4uYg\n" +
                "TQr04zvF/oYNkVfRRxWLQjj8jLAKnqOiaTPhr7XgOadNLWlggMZXqKvQhAahv3gu\n" +
                "ei/A/oAfSEx/i7ZffEv/bb7SKSuaukXNSxgBwpzMdaAew1nDTrSiH/wYwTMF0hgD\n" +
                "49JBboOogvlz4qELcX8GGXQcnr9/cbABetkLH5r0gZHyAGb/PLRzs2Ur3g4UrntK\n" +
                "lXY5tfr415D9m6CQJk42GKUlWjYQ77X6yFwc2RDWrmF6QvfWHDAXQ0eJUy+iqsMl\n" +
                "0WgrBAIJPUBh8W2p/KYg0BpLwKSXR6xC0qEgkoV8vpTZZc+11UvFKT/4pXdJGnWm\n" +
                "g5sbEhDT5FrT4GYNN3bBwkTLzK5ZzyvFLWJn0mdXrosW9WOR9gK22yU+BtijzHtm\n" +
                "H3myVtamjnmmDZp6UNvGEJ2x4mxHGP/5w7D6ftxRVDkDyEZd5v0NvMCn41FRx9E1\n" +
                "zI58bix8iT5TmD8J98EdkyptynNwsJz5eizn8/L7wiOZX8kgmlI3jU7BOikoMHDM\n" +
                "C6ojpULuVtd2x5gvLjq7mug5E6ILmh0JzOHhjgfIDm2vW71rLi+OPZlsGA0JViyz\n" +
                "EIQyARxnEXvDtY4=\n" +
                "-----END CERTIFICATE-----\n";
        String robca = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDxzCCAq+gAwIBAgIJAM5qKJz7nFUjMA0GCSqGSIb3DQEBBQUAMEsxCzAJBgNV\n" +
                "BAYTAkNaMQ0wCwYDVQQHDARCcm5vMQ8wDQYDVQQKDAZZIFNvZnQxDDAKBgNVBAsM\n" +
                "A1JuRDEOMAwGA1UEAwwFU2FmZVEwHhcNMTMxMTIwMjE1MDA5WhcNMjMxMTE4MjE1\n" +
                "MDEwWjBLMQswCQYDVQQGEwJDWjENMAsGA1UEBwwEQnJubzEPMA0GA1UECgwGWSBT\n" +
                "b2Z0MQwwCgYDVQQLDANSbkQxDjAMBgNVBAMMBVNhZmVRMIIBIjANBgkqhkiG9w0B\n" +
                "AQEFAAOCAQ8AMIIBCgKCAQEAmg8c05R/QP/ZTK1cRJuLMUQsSBd7gVarLZqR9v7W\n" +
                "gPlzcAI55LTB+q1wMrwCjpJwxLUCC/GBe/6RE9zwCVPUJR0stB+tBbdOLIMOxu/u\n" +
                "Scco6Um2fyjkS5eaBr1JxTcNA91r2IVqmCooTCAbHBY92EvkJ+z/Ys6tJG6f5UCg\n" +
                "IC+OQ45HbIh3paDu1s6vwuDAH8s+FOkRkrRv+A5diQK3YD2/L8xlEioLz7Zd7ea+\n" +
                "zRDI82ExBskMY17NmRaVWtp6DLxqh0hfKBSSHKydZF+gVlVIHWl0MrmuBVb/J4z1\n" +
                "2hjaoi1SZDaH44mP4N+K0Khji6elxxvJ+RHsOz56fYOIuQIDAQABo4GtMIGqMB0G\n" +
                "A1UdDgQWBBTz5YZo/3erV8osZCbJUdJoXC0tWTB7BgNVHSMEdDBygBTz5YZo/3er\n" +
                "V8osZCbJUdJoXC0tWaFPpE0wSzELMAkGA1UEBhMCQ1oxDTALBgNVBAcMBEJybm8x\n" +
                "DzANBgNVBAoMBlkgU29mdDEMMAoGA1UECwwDUm5EMQ4wDAYDVQQDDAVTYWZlUYIJ\n" +
                "AM5qKJz7nFUjMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBADEMS2wG\n" +
                "RpIL8bQuTRaqaq5z4pszqAucjdWlW8Pq/+YluF1ThYWUD9kJv18cBKl2NAz2QX2Q\n" +
                "shw7on+zZvG3K7ypkpa4ME+qAnazi3EzEEALi30YpWour1fqMJe/UmBQTn866V90\n" +
                "fMUSmw5U/na4cfNMzpfRbXyCnLDFg4VmbjO/y7gTLNeOnpCzoVTb9qr3AR5HDujN\n" +
                "vWG3h2tJsYGzw/1dqru70RJlAdxsF5ctPLTjhHRox0RzDuUQX9YIKeqBz7VNb2RE\n" +
                "82Rd3p5eyV6D9bEaTfbRVU6d6aWQAaobsBZ5oyWrqPf4FNPwUqd/YRoAQO4RKApg\n" +
                "WuzbOqq16bw1cpA=\n" +
                "-----END CERTIFICATE-----\n";
        String rootCert = "-----BEGIN CERTIFICATE-----\n" +
                "MIIF1jCCBL6gAwIBAgITNwAAFsr+WPSF4/INRgACAAAWyjANBgkqhkiG9w0BAQsF\n" +
                "ADA/MRUwEwYKCZImiZPyLGQBGRYFbG9jYWwxFTATBgoJkiaJk/IsZAEZFgV5c29m\n" +
                "dDEPMA0GA1UEAxMGc3VwaWNlMB4XDTE3MDMzMTEyMTkxNVoXDTIyMDExODEyMzcz\n" +
                "NVowHTEbMBkGA1UEAxMSc2FmZXE2Lnlzb2Z0LmxvY2FsMIIBIjANBgkqhkiG9w0B\n" +
                "AQEFAAOCAQ8AMIIBCgKCAQEAoYZxF5I40nPsKdQjBtdMlw9g36fXseH66UB5P5k0\n" +
                "3qLnV1cWKOz51YDIj4zljA0PUqLvK0qxXdrvD8UFpE7YfUuWlCcbGRRVC67VCaYc\n" +
                "OksLQlw2EsxGGOdlMJ+lJJvzKSGiK4PzyWBaIgRASPFiI7gfDzTGp1zxRwaCs4nM\n" +
                "lDvOYJzL07teEIBBrQ05ey7rtuIZo5f9J7mtMg5A5pPeZF2kRT+VDrnfJu4kbNWF\n" +
                "P0jg0oLGSmYNE0Zk6qCNUTWzwM8esiY6qsiA0op3q0lU8+ydP/hDU4u9qZF3YtxL\n" +
                "WzEuU4OiINWf057o2dzlMrvcgNJ9ddpBLA65cLOvn2DSDwIDAQABo4IC6zCCAucw\n" +
                "PAYJKwYBBAGCNxUHBC8wLQYlKwYBBAGCNxUIh9W1AYbGukCD/ZULvo1n0udYgXeD\n" +
                "/udnh8/FNgIBZAIBEjATBgNVHSUEDDAKBggrBgEFBQcDATALBgNVHQ8EBAMCBaAw\n" +
                "GwYJKwYBBAGCNxUKBA4wDDAKBggrBgEFBQcDATAdBgNVHQ4EFgQUd3dPA1MxPBfg\n" +
                "vuZLBicgH4S9qpYwMgYDVR0RBCswKYITc3EtYnJuby55c29mdC5sb2NhbIISc2Fm\n" +
                "ZXE2Lnlzb2Z0LmxvY2FsMB8GA1UdIwQYMBaAFL5I0JzcCTvocP2hMRqB67oMU3Qm\n" +
                "MIHsBgNVHR8EgeQwgeEwgd6ggduggdiGgahsZGFwOi8vL0NOPXN1cGljZSxDTj1j\n" +
                "YSxDTj1DRFAsQ049UHVibGljJTIwS2V5JTIwU2VydmljZXMsQ049U2VydmljZXMs\n" +
                "Q049Q29uZmlndXJhdGlvbixEQz15c29mdCxEQz1sb2NhbD9jZXJ0aWZpY2F0ZVJl\n" +
                "dm9jYXRpb25MaXN0P2Jhc2U/b2JqZWN0Q2xhc3M9Y1JMRGlzdHJpYnV0aW9uUG9p\n" +
                "bnSGK2h0dHA6Ly9jYS55c29mdC5sb2NhbC9DZXJ0RW5yb2xsL3N1cGljZS5jcmww\n" +
                "ggEDBggrBgEFBQcBAQSB9jCB8zCBpQYIKwYBBQUHMAKGgZhsZGFwOi8vL0NOPXN1\n" +
                "cGljZSxDTj1BSUEsQ049UHVibGljJTIwS2V5JTIwU2VydmljZXMsQ049U2Vydmlj\n" +
                "ZXMsQ049Q29uZmlndXJhdGlvbixEQz15c29mdCxEQz1sb2NhbD9jQUNlcnRpZmlj\n" +
                "YXRlP2Jhc2U/b2JqZWN0Q2xhc3M9Y2VydGlmaWNhdGlvbkF1dGhvcml0eTBJBggr\n" +
                "BgEFBQcwAoY9aHR0cDovL2NhLnlzb2Z0LmxvY2FsL0NlcnRFbnJvbGwvY2EueXNv\n" +
                "ZnQubG9jYWxfc3VwaWNlKDIpLmNydDANBgkqhkiG9w0BAQsFAAOCAQEAnOL62M0y\n" +
                "1KqDstJmQawEMSBeKoEA+MtfTa6R0xTfDeVWcLkmeu3jc1vmFP955UMxUnMg2Ihz\n" +
                "HjEmOCCx2ifRSxtpdVeNAEoD/lh7LQAjeC6Me23FS8kkT9dIRPVPaSxc6CIXCuuT\n" +
                "SHkkbGU+RjBFgrD1ukeAd9uwtEA1V4cKgSnQPQvcFm8VLrprb1UDmVwZkug765I9\n" +
                "Hv6Z5bTtm0Rsmpshxq40X228CXor+idamI2XaAi9QOLNKE+zyh9oguAloF+nDgPU\n" +
                "WKCxrF137xoOAzwPxVA9pJTwa3Bj7i4NlIKXOvOyaQyi277kqifdJWUl5m3DWs8E\n" +
                "QY39XIciojfVhA==\n" +
                "-----END CERTIFICATE-----\n";
        return new Buffer()
                //.writeUtf8(comodoRsaCertificationAuthority)
                //.writeUtf8(entrustRootCertificateAuthority)
                .writeUtf8(supice)
                .writeUtf8(rndca3)
                .writeUtf8(robca)
                .writeUtf8(rootCert)
                //.writeUtf8(rndca2)
                //.writeUtf8(rndca3)
                .inputStream();
    }

    /**
     * Returns a trust manager that trusts {@code certificates} and none other. HTTPS services whose
     * certificates have not been signed by these certificates will fail with a {@code
     * SSLHandshakeException}.
     *
     * <p>This can be used to replace the host platform's built-in trusted certificates with a custom
     * set. This is useful in development where certificate authority-trusted certificates aren't
     * available. Or in production, to avoid reliance on third-party certificate authorities.
     *
     * <p>See also {@link CertificatePinner}, which can limit trusted certificates while still using
     * the host platform's built-in trust store.
     *
     * <h3>Warning: Customizing Trusted Certificates is Dangerous!</h3>
     *
     * <p>Relying on your own trusted certificates limits your server team's ability to update their
     * TLS certificates. By installing a specific set of trusted certificates, you take on additional
     * operational complexity and limit your ability to migrate between certificate authorities. Do
     * not use custom trusted certificates in production without the blessing of your server's TLS
     * administrator.
     */
    private X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {


        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

}