/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2016. All rights reserved.
 */

package com.huotu.hotcms.widget.productList;

import com.huotu.hotcms.service.entity.Article;
import com.huotu.hotcms.service.entity.MallProductCategory;
import com.huotu.hotcms.service.repository.ArticleRepository;
import com.huotu.hotcms.service.repository.MallProductCategoryRepository;
import com.huotu.hotcms.widget.CMSContext;
import com.huotu.hotcms.widget.ComponentProperties;
import com.huotu.hotcms.widget.Widget;
import com.huotu.hotcms.widget.WidgetStyle;
import com.huotu.widget.test.Editor;
import com.huotu.widget.test.WidgetTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author CJ
 */
public class TestWidgetInfo extends WidgetTest {

    @Override
    protected boolean printPageSource() {
        return false;
    }

    @Override
    protected void editorWork(Widget widget, Editor editor, Supplier<Map<String, Object>> currentWidgetProperties) {
        if (widget instanceof WidgetInfo) {
            WidgetInfo widgetInfo = (WidgetInfo) widget;
            MallProductCategory mallProductCategory = widgetInfo.initMallProductCategory(null);
            Article article = widgetInfo.initArticle();
            editor.chooseCategory(WidgetInfo.MALL_PRODUCT_SERIAL,mallProductCategory);
            editor.chooseArticle(WidgetInfo.LEFT_ARTICLE_SERIAL,article);
            Map map = currentWidgetProperties.get();
            assertThat(mallProductCategory.getSerial()).isEqualTo(map.get(WidgetInfo.MALL_PRODUCT_SERIAL));
            assertThat(article.getSerial()).isEqualTo(map.get(WidgetInfo.LEFT_ARTICLE_SERIAL));
        }
    }

    @Override
    protected void browseWork(Widget widget, WidgetStyle style, Function<ComponentProperties, WebElement> uiChanger)
            throws IOException {
        ComponentProperties properties = widget.defaultProperties(resourceService);
        WebElement webElement = uiChanger.apply(properties);
        ArticleRepository articleRepository = widget.getCMSServiceFromCMSContext(ArticleRepository.class);
        String leftSerial = (String) properties.get(WidgetInfo.LEFT_ARTICLE_SERIAL);
        String rightSerial = (String) properties.get(WidgetInfo.RIGHT_ARTICLE_SERIAL);
        Article leftArticle = articleRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(),
                leftSerial);
        Article rightArticle = articleRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(),
                rightSerial);

        String mallProductSerial = (String) properties.get(WidgetInfo.MALL_PRODUCT_SERIAL);
        MallProductCategoryRepository mallProductCategoryRepository = widget.getCMSServiceFromCMSContext
                (MallProductCategoryRepository.class);
        List<MallProductCategory> dataList = mallProductCategoryRepository.findBySiteAndParent_Serial(CMSContext
                .RequestContext().getSite(), mallProductSerial);
        assertThat(dataList.size()).isEqualTo(webElement.findElements(By.className("mallProduct")).size());
        assertThat(leftArticle.getContent()).isEqualTo(webElement.findElement(By.className("leftArticle")).getText());
        assertThat(rightArticle.getContent()).isEqualTo(webElement.findElement(By.className("rightArticle")).getText());

    }

    @Override
    protected void editorBrowseWork(Widget widget, Function<ComponentProperties, WebElement> uiChanger
            , Supplier<Map<String, Object>> currentWidgetProperties) throws IOException {
        ComponentProperties properties = widget.defaultProperties(resourceService);
        WebElement webElement = uiChanger.apply(properties);

    }
}
