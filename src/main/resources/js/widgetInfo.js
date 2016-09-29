/**
 * Created by lhx on 2016/8/11.
 */
CMSWidgets.initWidget({
// 编辑器相关
    editor: {
        properties: null,
        saveComponent: function (onSuccess, onFailed) {
            onSuccess(this.properties);
            return this.properties;
        },
        open: function (globalId) {
            this.properties = widgetProperties(globalId);

        },
        close: function (globalId) {

        }
    }
});
