/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2016. All rights reserved.
 */

package com.huotu.hotcms.widget.productList;

import com.huotu.hotcms.service.common.ContentType;
import com.huotu.hotcms.service.entity.Article;
import com.huotu.hotcms.service.entity.Category;
import com.huotu.hotcms.service.entity.MallProductCategory;
import com.huotu.hotcms.service.model.MallProductCategoryModel;
import com.huotu.hotcms.service.repository.ArticleRepository;
import com.huotu.hotcms.service.repository.CategoryRepository;
import com.huotu.hotcms.service.repository.MallProductCategoryRepository;
import com.huotu.hotcms.service.service.CategoryService;
import com.huotu.hotcms.service.service.ContentService;
import com.huotu.hotcms.widget.CMSContext;
import com.huotu.hotcms.widget.ComponentProperties;
import com.huotu.hotcms.widget.PreProcessWidget;
import com.huotu.hotcms.widget.Widget;
import com.huotu.hotcms.widget.WidgetStyle;
import com.huotu.huobanplus.common.entity.Goods;
import com.huotu.huobanplus.sdk.common.repository.GoodsRestRepository;
import me.jiangcai.lib.resource.service.ResourceService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * @author CJ
 */
public class WidgetInfo implements Widget, PreProcessWidget {
    private static final String LEFT_ARTICLE_SERIAL = "leftArticleSerial";
    private static final String RIGHT_ARTICLE_SERIAL = "rightArticleSerial";
    private static final String RIGHT_TITLE = "title";
    private static final String MALL_PRODUCT_SERIAL = "mallProductSerial";
    private static final String LEFT_CONTENT = "leftArticle";
    private static final String RIGHT_CONTENT = "rightArticle";
    private static final String MALL_PRODUCT_DATA_LIST = "mallProductDataList";
    private static final String MALL_PRODUCT_CATEGORY = "mallProductCategory";

    @Override
    public String groupId() {
        return "com.huotu.hotcms.widget.productList";
    }

    @Override
    public String widgetId() {
        return "productList";
    }

    @Override
    public String name(Locale locale) {
        if (locale.equals(Locale.CHINA)) {
            return "产品列表";
        }
        return "productList";
    }

    @Override
    public String description(Locale locale) {
        if (locale.equals(Locale.CHINA)) {
            return "这是一个产品列表组件，你可以对组件进行自定义修改。";
        }
        return "This is a productList,  you can make custom change the component.";
    }

    @Override
    public String dependVersion() {
        return "1.0-SNAPSHOT";
    }

    @Override
    public WidgetStyle[] styles() {
        return new WidgetStyle[]{new DefaultWidgetStyle()};
    }

    @Override
    public Resource widgetDependencyContent(MediaType mediaType) {
        if (mediaType.equals(Widget.Javascript))
            return new ClassPathResource("js/widgetInfo.js", getClass().getClassLoader());
        return null;
    }

    @Override
    public Map<String, Resource> publicResources() {
        Map<String, Resource> map = new HashMap<>();
        map.put("thumbnail/defaultStyleThumbnail.png", new ClassPathResource("thumbnail/defaultStyleThumbnail.png"
                , getClass().getClassLoader()));
        return map;
    }

    @Override
    public void valid(String styleId, ComponentProperties componentProperties) throws IllegalArgumentException {
        WidgetStyle style = WidgetStyle.styleByID(this, styleId);
        //加入控件独有的属性验证
        String leftSerial = (String) componentProperties.get(LEFT_ARTICLE_SERIAL);
        String productSerial = (String) componentProperties.get(MALL_PRODUCT_SERIAL);
        if (leftSerial == null || productSerial == null || leftSerial.equals("") || productSerial.equals("")) {
            throw new IllegalArgumentException("产品数据源和左侧文章内容不能为空");
        }

    }

    @Override
    public Class springConfigClass() {
        return null;
    }


    @Override
    public ComponentProperties defaultProperties(ResourceService resourceService) throws IOException {
        ComponentProperties properties = new ComponentProperties();

        ArticleRepository articleRepository = CMSContext.RequestContext().getWebApplicationContext()
                .getBean(ArticleRepository.class);
        List<Article> list = articleRepository.findByCategory_Site(CMSContext.RequestContext().getSite());
        if (list.isEmpty()) {
            Article article = initArticle();
            properties.put(LEFT_ARTICLE_SERIAL, article.getSerial());
            properties.put(RIGHT_ARTICLE_SERIAL, article.getSerial());
            properties.put(RIGHT_TITLE, article.getTitle());
        } else {
            properties.put(LEFT_ARTICLE_SERIAL, list.get(0).getSerial());
            properties.put(RIGHT_ARTICLE_SERIAL, list.get(0).getSerial());
            properties.put(RIGHT_TITLE, list.get(0).getTitle());
        }

        //查找商城产品数据源
        MallProductCategoryRepository mallProductCategoryRepository = CMSContext.RequestContext()
                .getWebApplicationContext().getBean(MallProductCategoryRepository.class);
        List<MallProductCategory> mallProductCategoryList = mallProductCategoryRepository.findBySite(CMSContext
                .RequestContext().getSite());
        if (mallProductCategoryList.isEmpty()) {
            MallProductCategory mallProductCategory = initMallProductCategory(null);
            initMallProductCategory(mallProductCategory);
            properties.put(MALL_PRODUCT_SERIAL, mallProductCategory.getSerial());
        } else {
            mallProductCategoryList.get(0).getParent();
            properties.put(MALL_PRODUCT_SERIAL, mallProductCategoryList.get(0).getSerial());
        }
        return properties;
    }

    @Override
    public void prepareContext(WidgetStyle style, ComponentProperties properties, Map<String, Object> variables
            , Map<String, String> parameters) {
        ArticleRepository articleRepository = CMSContext.RequestContext().getWebApplicationContext().getBean
                (ArticleRepository.class);
        String leftSerial = (String) variables.get(LEFT_ARTICLE_SERIAL);
        String rightSerial = (String) variables.get(RIGHT_ARTICLE_SERIAL);
        Article leftArticle = articleRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(),
                leftSerial);
        Article rightArticle = articleRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(),
                rightSerial);
        variables.put(LEFT_CONTENT,leftArticle);
        variables.put(RIGHT_CONTENT,rightArticle);

        String mallProductSerial = (String) variables.get(MALL_PRODUCT_SERIAL);
        MallProductCategoryRepository mallProductCategoryRepository = CMSContext.RequestContext()
                .getWebApplicationContext().getBean(MallProductCategoryRepository.class);
        List<MallProductCategory> dataList = mallProductCategoryRepository.findBySiteAndParent_Serial(CMSContext
                .RequestContext().getSite(),mallProductSerial);
        GoodsRestRepository goodsRestRepository = CMSContext.RequestContext().getWebApplicationContext().getBean
                (GoodsRestRepository.class);
        Pageable pageable = new PageRequest(0,8, Sort.Direction.ASC,"id");
        List<MallProductCategoryModel> list = new ArrayList<>();
        for (MallProductCategory mallProductCategory :dataList){
            try {
                Page<Goods> goodsPage=goodsRestRepository.search(mallProductCategory.getSite().getOwner()
                                .getCustomerId(),
                        mallProductCategory
                        .getMallCategoryId(),null,mallProductCategory.getMallBrandId(),mallProductCategory.getMinPrice()
                        ,mallProductCategory.getMaxPrice(),null,mallProductCategory.getGoodTitle(),mallProductCategory
                                .getSalesCount(),mallProductCategory.getStock(),mallProductCategory.getDisabled(),
                        mallProductCategory.getMarkerTable(),pageable);
                MallProductCategoryModel mallProductCategoryModel = mallProductCategory.toMallProductCategoryModel();
                mallProductCategoryModel.setMallGoodsPage(goodsPage);
                list.add(mallProductCategoryModel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MallProductCategory mallProductCategory = mallProductCategoryRepository.findBySerial(mallProductSerial);
        variables.put(MALL_PRODUCT_CATEGORY,mallProductCategory);
        variables.put(MALL_PRODUCT_DATA_LIST,list);

    }

    private MallProductCategory initMallProductCategory(MallProductCategory parent) {
        CategoryService categoryService = CMSContext.RequestContext().getWebApplicationContext()
                .getBean(CategoryService.class);
        MallProductCategoryRepository mallProductCategoryRepository = CMSContext.RequestContext()
                .getWebApplicationContext().getBean(MallProductCategoryRepository.class);
        MallProductCategory mallProductCategory = new MallProductCategory();
        mallProductCategory.setGoodTitle("");
        mallProductCategory.setSite(CMSContext.RequestContext().getSite());
        mallProductCategory.setName("商城产品数据源");
        mallProductCategory.setCreateTime(LocalDateTime.now());
        mallProductCategory.setContentType(ContentType.MallProduct);
        mallProductCategory.setParent(parent);
        categoryService.init(mallProductCategory);
        mallProductCategoryRepository.save(mallProductCategory);
        return mallProductCategory;
    }


    private Article initArticle() {
        CategoryService categoryService = CMSContext.RequestContext().getWebApplicationContext()
                .getBean(CategoryService.class);
        CategoryRepository categoryRepository = CMSContext.RequestContext().getWebApplicationContext().getBean
                (CategoryRepository.class);
        ArticleRepository articleRepository = CMSContext.RequestContext().getWebApplicationContext().getBean
                (ArticleRepository.class);
        ContentService contentService = CMSContext.RequestContext().getWebApplicationContext().getBean(ContentService
                .class);
        Category category = new Category();
        category.setName("文章数据源");
        category.setSite(CMSContext.RequestContext().getSite());
        category.setContentType(ContentType.Article);
        category.setCreateTime(LocalDateTime.now());
        categoryService.init(category);
        categoryRepository.save(category);
        Article article = new Article();
        article.setContent("文章内容");
        article.setCreateTime(LocalDateTime.now());
        article.setTitle("文章标题");
        article.setCategory(category);
        article.setAuthor("系统");
        contentService.init(article);
        articleRepository.save(article);
        return article;
    }


}
