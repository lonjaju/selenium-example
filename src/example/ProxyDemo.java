import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProxyDemo {
    private static void createProxyAuthExtension(String proxyHost, String proxyPort, String proxyUser, String proxyPass, String pluginPath) throws IOException {

        String manifestJson = """
                {
                    "version": "1.0.0",
                    "manifest_version": 2,
                    "name": "Abuyun Proxy",
                    "permissions": [
                        "proxy",
                        "tabs",
                        "unlimitedStorage",
                        "storage",
                        "<all_urls>",
                        "webRequest",
                        "webRequestBlocking"
                    ],
                    "background": {
                        "scripts": ["background.js"]
                    },
                    "minimum_chrome_version":"22.0.0"
                }
                """;

        String backgroundJs = """
                var config = {
                    mode: "fixed_servers",
                    rules: {
                        singleProxy: {
                            scheme: "${scheme}",
                            host: "${host}",
                            port: parseInt(${port})
                        },
                        bypassList: ["foobar.com"]
                    }
                  };

                chrome.proxy.settings.set({value: config, scope: "regular"}, function() {});

                function callbackFn(details) {
                    return {
                        authCredentials: {
                            username: "${username}",
                            password: "${password}"
                        }
                    };
                }

                chrome.webRequest.onAuthRequired.addListener(
                    callbackFn,
                    {urls: ["<all_urls>"]},
                    ['blocking']
                );
                """
                .replace("${host}", proxyHost)
                .replace("${port}", proxyPort)
                .replace("${username}", proxyUser)
                .replace("${password}", proxyPass)
                .replace("${scheme}", "http");

        try (ZipOutputStream zp = new ZipOutputStream(new FileOutputStream(pluginPath))) {
            zp.putNextEntry(new ZipEntry("manifest.json"));
            zp.write(manifestJson.getBytes());
            zp.closeEntry();
            zp.putNextEntry(new ZipEntry("background.js"));
            zp.write(backgroundJs.getBytes());
            zp.closeEntry();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String proxyHost = "http-dyn.abuyun.com";
        String proxyPort = "9020";
        // 隧道密码
        // TODO: 改成用户后台隧道列表中的值
        String proxyUser = "H01234567890123D";
        String proxyPass = "0123456789012345";
        // TODO: 可根据实际情况调整
        // 生成用户名和密码对应的浏览器插件压缩包用于后面启动加载
        String pluginPath = "/tmp/http-dyn.abuyun.com_9020.zip";
        createProxyAuthExtension(proxyHost, proxyPort, proxyUser, proxyPass, pluginPath);

        // chromedriver
        // TODO: 改成实际路径
        // @see https://chromedriver.chromium.org/downloads
        System.setProperty("webdriver.chrome.driver", "/tmp/chromedriver");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setHeadless(false);
        chromeOptions.addArguments("--start-maximized");
        chromeOptions.addExtensions(new File(pluginPath));
        chromeOptions.setHeadless(false);

        WebDriver driver = new ChromeDriver(chromeOptions);

        driver.get("http://myip.top");

        // 休眠30秒
        Thread.sleep(30000);

        // 关闭浏览器
        driver.quit();
    }
}

