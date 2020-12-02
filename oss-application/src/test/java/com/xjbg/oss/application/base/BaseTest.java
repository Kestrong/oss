package com.xjbg.oss.application.base;

import com.xjbg.oss.application.OssTestApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author kesc
 * @date 2020-04-03 11:14
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OssTestApplication.class)
@ActiveProfiles(value = "local")
public abstract class BaseTest {

}
