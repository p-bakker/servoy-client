name: 'svy-tabpanel',
displayName: 'Tab panel',
definition: 'servoydefault/tabpanel/tabpanel.js',
model:
{
        horizontalAlignment : {type:'number', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        enabled : 'boolean', 
        visible : 'boolean', 
        tabSeq : 'tabseq', 
        closeOnTabs : 'boolean', 
        scrollTabs : 'boolean', 
        styleClass : 'string', 
        transparent : 'boolean', 
        selectedTabColor : 'color', 
        fontType : 'font', 
        printable : 'boolean', 
        borderType : 'border', 
        size : 'dimension', 
        tabOrientation : 'number', 
        location : 'point', 
        foreground : 'color', 
        background : 'color' 
},
handlers:
{
        onChangeMethodID : 'function', 
        onTabChangeMethodID : 'function' 
},
api:
{
        addTab:{
            returns: 'boolean',
            parameters:[ {'form/formname':'object []','optional':'false'}, {'name':'object','optional':'true'}, {'tabText':'object','optional':'true'}, {'tooltip':'object','optional':'true'}, {'iconURL':'object','optional':'true'}, {'fg':'object','optional':'true'}, {'bg':'object','optional':'true'}, {'relatedfoundset/relationname':'object','optional':'true'}, {'index':'object','optional':'true'}]
        }, 
        getAbsoluteFormLocationY:{
            returns: 'number',
            parameters:[]
        }, 
        getClientProperty:{
            returns: 'object',
            parameters:[ {'key':'object','optional':'false'}]
        }, 
        getDesignTimeProperty:{
            returns: 'object',
            parameters:[ {'unnamed_0':'string','optional':'false'}]
        }, 
        getElementType:{
            returns: 'string',
            parameters:[]
        }, 
        getHeight:{
            returns: 'number',
            parameters:[]
        }, 
        getLocationX:{
            returns: 'number',
            parameters:[]
        }, 
        getLocationY:{
            returns: 'number',
            parameters:[]
        }, 
        getMaxTabIndex:{
            returns: 'number',
            parameters:[]
        }, 
        getMnemonicAt:{
            returns: 'string',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getSelectedTabFormName:{
            returns: 'string',
            parameters:[]
        }, 
        getTabBGColorAt:{
            returns: 'string',
            parameters:[ {'unnamed_0':'number','optional':'false'}]
        }, 
        getTabFGColorAt:{
            returns: 'string',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        getTabFormNameAt:{
            returns: 'string',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        getTabNameAt:{
            returns: 'string',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        getTabRelationNameAt:{
            returns: 'string',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        getTabTextAt:{
            returns: 'string',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        getWidth:{
            returns: 'number',
            parameters:[]
        }, 
        isTabEnabled:{
            returns: 'boolean',
            parameters:[ {'unnamed_0':'number','optional':'false'}]
        }, 
        isTabEnabledAt:{
            returns: 'boolean',
            parameters:[ {'i':'number','optional':'false'}]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        removeAllTabs:{
            returns: 'boolean',
            parameters:[]
        }, 
        removeTabAt:{
            returns: 'boolean',
            parameters:[ {'index':'number','optional':'false'}]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'number','optional':'false'}, {'y':'number','optional':'false'}]
        }, 
        setMnemonicAt:{
            returns: 'void',
            parameters:[ {'index':'number','optional':'false'}, {'text':'string','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'number','optional':'false'}, {'height':'number','optional':'false'}]
        }, 
        setTabBGColorAt:{
            returns: 'void',
            parameters:[ {'unnamed_0':'number','optional':'false'}, {'unnamed_1':'string','optional':'false'}]
        }, 
        setTabEnabled:{
            returns: 'void',
            parameters:[ {'unnamed_0':'number','optional':'false'}, {'unnamed_1':'boolean','optional':'false'}]
        }, 
        setTabEnabledAt:{
            returns: 'void',
            parameters:[ {'i':'number','optional':'false'}, {'b':'boolean','optional':'false'}]
        }, 
        setTabFGColorAt:{
            returns: 'void',
            parameters:[ {'i':'number','optional':'false'}, {'s':'string','optional':'false'}]
        }, 
        setTabTextAt:{
            returns: 'void',
            parameters:[ {'index':'number','optional':'false'}, {'text':'string','optional':'false'}]
        } 
}