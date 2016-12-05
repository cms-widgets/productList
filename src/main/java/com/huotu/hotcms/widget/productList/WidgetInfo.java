/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2016. All rights reserved.
 */

package com.huotu.hotcms.widget.productList;

import com.huotu.hotcms.service.common.ArticleSource;
import com.huotu.hotcms.service.common.ContentType;
import com.huotu.hotcms.service.entity.Article;
import com.huotu.hotcms.service.entity.Category;
import com.huotu.hotcms.service.entity.MallProductCategory;
import com.huotu.hotcms.service.exception.PageNotFoundException;
import com.huotu.hotcms.service.model.MallProductCategoryModel;
import com.huotu.hotcms.service.repository.ArticleRepository;
import com.huotu.hotcms.service.repository.CategoryRepository;
import com.huotu.hotcms.service.repository.MallProductCategoryRepository;
import com.huotu.hotcms.service.service.CategoryService;
import com.huotu.hotcms.service.service.ContentService;
import com.huotu.hotcms.service.service.MallService;
import com.huotu.hotcms.widget.CMSContext;
import com.huotu.hotcms.widget.ComponentProperties;
import com.huotu.hotcms.widget.PreProcessWidget;
import com.huotu.hotcms.widget.Widget;
import com.huotu.hotcms.widget.WidgetStyle;
import com.huotu.hotcms.widget.entity.PageInfo;
import com.huotu.hotcms.widget.service.PageService;
import com.huotu.huobanplus.common.entity.Goods;
import com.huotu.huobanplus.sdk.common.repository.GoodsRestRepository;
import com.huotu.huobanplus.sdk.common.repository.MerchantRestRepository;
import me.jiangcai.lib.resource.service.ResourceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public static final String LEFT_ARTICLE_SERIAL = "leftArticleSerial";
    public static final String RIGHT_ARTICLE_SERIAL = "rightArticleSerial";
    public static final String MALL_PRODUCT_SERIAL = "mallProductSerial";
    private static final Log log = LogFactory.getLog(WidgetInfo.class);
    private static final String RIGHT_TITLE = "title";
    private static final String LEFT_CONTENT = "leftArticle";
    private static final String RIGHT_CONTENT = "rightArticle";
    private static final String MALL_PRODUCT_DATA_LIST = "mallProductDataList";
    private static final String MALL_PRODUCT_CATEGORY = "mallProductCategory";
    @Autowired
    private MerchantRestRepository merchantRestRepository;

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
        return "1.0";
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
    public boolean disabled() {
        return CMSContext.RequestContext().getSite().getOwner() != null && CMSContext.RequestContext().getSite().getOwner().getCustomerId() != null;
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
        //todo 过滤删除的数据源
        List<MallProductCategory> mallProductCategoryList = mallProductCategoryRepository.findBySiteAndDeletedFalse(CMSContext
                .RequestContext().getSite());
        if (mallProductCategoryList.isEmpty()) {
            MallProductCategory mallProductCategory = initMallProductCategory(null);
            initMallProductCategory(mallProductCategory);
            properties.put(MALL_PRODUCT_SERIAL, mallProductCategory.getSerial());
        } else {
            properties.put(MALL_PRODUCT_SERIAL, mallProductCategoryList.get(0).getSerial());
        }
        return properties;
    }

    @Override
    public void prepareContext(WidgetStyle style, ComponentProperties properties, Map<String, Object> variables
            , Map<String, String> parameters) {
        ArticleRepository articleRepository = getCMSServiceFromCMSContext(ArticleRepository.class);
        String leftSerial = (String) variables.get(LEFT_ARTICLE_SERIAL);
        String rightSerial = (String) variables.get(RIGHT_ARTICLE_SERIAL);
        Article leftArticle = articleRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(),
                leftSerial);
        Article rightArticle = articleRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(),
                rightSerial);
        variables.put(LEFT_CONTENT, leftArticle);
        variables.put(RIGHT_CONTENT, rightArticle);

        String mallProductSerial = (String) variables.get(MALL_PRODUCT_SERIAL);
        MallProductCategoryRepository mallProductCategoryRepository = getCMSServiceFromCMSContext(MallProductCategoryRepository.class);
        List<MallProductCategory> dataList = mallProductCategoryRepository.findBySiteAndParent_SerialAndDeletedFalse(CMSContext
                .RequestContext().getSite(), mallProductSerial);
        GoodsRestRepository goodsRestRepository = getCMSServiceFromCMSContext(GoodsRestRepository.class);
        Pageable pageable = new PageRequest(0, 8, Sort.Direction.ASC, "id");
        List<MallProductCategoryModel> list = new ArrayList<>();
        for (MallProductCategory mallProductCategory : dataList) {
            MallProductCategoryModel mallProductCategoryModel = mallProductCategory.toMallProductCategoryModel();
            try {
                Page<Goods> goodsPage = goodsRestRepository.search(mallProductCategory.getSite().getOwner()
                                .getCustomerId(),
                        mallProductCategory
                                .getMallCategoryId(), null, mallProductCategory.getMallBrandId(), mallProductCategory.getMinPrice()
                        , mallProductCategory.getMaxPrice(), null, mallProductCategory.getGoodTitle(), mallProductCategory
                                .getSalesCount(), mallProductCategory.getStock(), false,
                        true, pageable);
//                setContentURI(variables,mallProductCategory);
                mallProductCategoryModel.setMallGoodsPage(goodsPage);
                list.add(mallProductCategoryModel);
            } catch (IOException e) {
                log.error("通讯异常", e);
                list.add(mallProductCategoryModel);
            }
        }
        MallProductCategory mallProductCategory = mallProductCategoryRepository.findBySerial(mallProductSerial);
        variables.put(MALL_PRODUCT_CATEGORY, mallProductCategory);
        variables.put(MALL_PRODUCT_DATA_LIST, list);
        MallService mallService = getCMSServiceFromCMSContext(MallService.class);
        try {
            String domain = mallService.getMallDomain(CMSContext.RequestContext().getSite().getOwner());
            variables.put("goodDetailUrl", "http://" + domain + "/Mall/GoodDetail/" + CMSContext.RequestContext().getSite().getOwner().getCustomerId());
        } catch (IOException e) {
            log.error("通讯异常", e);
            variables.put("goodDetailUrl", "#");
        }

    }

    private void setContentURI(Map<String, Object> variables, MallProductCategory mallProductCategory) {
        try {
            PageInfo contentPage = getCMSServiceFromCMSContext(PageService.class)
                    .getClosestContentPage(mallProductCategory, (String) variables.get("uri"));
            mallProductCategory.setContentURI(contentPage.getPagePath());
        } catch (PageNotFoundException e) {
            log.warn("...", e);
            mallProductCategory.setContentURI((String) variables.get("uri"));
        }
    }

    public MallProductCategory initMallProductCategory(MallProductCategory parent) {
        CategoryService categoryService = getCMSServiceFromCMSContext(CategoryService.class);
        MallProductCategoryRepository mallProductCategoryRepository = getCMSServiceFromCMSContext(MallProductCategoryRepository.class);
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


    public Article initArticle() {
        CategoryService categoryService = getCMSServiceFromCMSContext(CategoryService.class);
        CategoryRepository categoryRepository = getCMSServiceFromCMSContext(CategoryRepository.class);
        ArticleRepository articleRepository = getCMSServiceFromCMSContext(ArticleRepository.class);
        ContentService contentService = getCMSServiceFromCMSContext(ContentService.class);
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
        article.setArticleSource(ArticleSource.ORIGINAL);
        article.setCategory(category);
        article.setAuthor("系统");
        contentService.init(article);
        articleRepository.save(article);
        return article;
    }


}
