name: 'svy-accordionpanel',
displayName: 'AccordionPanel',
definition: 'servoydefault/accordionpanel/accordionpanel.js',
model:
{
        horizontalAlignment : {type:'int', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
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
        tabOrientation : 'int', 
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
            parameters:[{'form/formname':'object []'},{'name':'object','optional':'true'},{'tabText':'object','optional':'true'},{'tooltip':'object','optional':'true'},{'iconURL':'object','optional':'true'},{'fg':'object','optional':'true'},{'bg':'object','optional':'true'},{'relatedfoundset/relationname':'object','optional':'true'},{'index':'object','optional':'true'}]
        }, 
        getAbsoluteFormLocationY:{
            returns: 'int',
                 }, 
        getClientProperty:{
            returns: 'object',
            parameters:[{'key':'object'}]
        }, 
        getDesignTimeProperty:{
            returns: 'object',
            parameters:[{'unnamed_0':'string'}]
        }, 
        getElementType:{
            returns: 'string',
                 }, 
        getHeight:{
            returns: 'int',
                 }, 
        getLocationX:{
            returns: 'int',
                 }, 
        getLocationY:{
            returns: 'int',
                 }, 
        getMaxTabIndex:{
            returns: 'int',
                 }, 
        getMnemonicAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getName:{
            returns: 'string',
                 }, 
        getTabFGColorAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabFormNameAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabNameAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabRelationNameAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabTextAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getWidth:{
            returns: 'int',
                 }, 
        isTabEnabledAt:{
            returns: 'boolean',
            parameters:[{'i':'int'}]
        }, 
        putClientProperty:{
            
            parameters:[{'key':'object'},{'value':'object'}]
        }, 
        removeAllTabs:{
            returns: 'boolean',
                 }, 
        removeTabAt:{
            returns: 'boolean',
            parameters:[{'index':'int'}]
        }, 
        setLocation:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setMnemonicAt:{
            
            parameters:[{'index':'int'},{'text':'string'}]
        }, 
        setSize:{
            
            parameters:[{'width':'int'},{'height':'int'}]
        }, 
        setTabEnabledAt:{
            
            parameters:[{'i':'int'},{'b':'boolean'}]
        }, 
        setTabFGColorAt:{
            
            parameters:[{'i':'int'},{'s':'string'}]
        }, 
        setTabTextAt:{
            
            parameters:[{'index':'int'},{'text':'string'}]
        } 
}