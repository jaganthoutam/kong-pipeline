package com.fts.api.kong

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
/**
 * KongManager class to model Kong operations
 */
class KongManager implements Serializable {

    def steps

    /** The protocol to be used to connect kong. https by default */
    def kongProtocol

    /** The host to be used to connect kong. */
    def kongHost

    /** The host to be used to admin kong. */
    def kongAdminHost

    /** The port to be used to connect kong. */
    def kongPort

    def kongUrl
    def kongAdminUrl

    def kongServicesUrl
    def kongRoutesUrl

    def KongManager(steps, kongProtocol, kongHost, kongAdminHost, kongPort){

        this.steps = steps
        if(kongProtocol != null){
            this.kongProtocol = kongProtocol
        }else{
            this.kongProtocol = 'https'
        }
        
        this.kongHost = kongHost
        this.kongAdminHost = kongAdminHost
        this.kongPort = kongPort

        def baseKongUrl = 'https'+'://'+this.kongHost
        def baseKongAdminUrl = 'https'+'://'+this.kongAdminHost

        this.kongUrl = this.kongProtocol+'://'+this.kongHost+':'+this.kongPort+'/'+this.kongApisUrl
        this.kongServicesUrl = baseKongAdminUrl+'/services/'
        this.kongRoutesUrl = baseKongAdminUrl+'/routes/'

    }

    /**
     * @param
     */
    def register(byApiName, stage, endpointName, endpointHost, endpointPath, endpointUpstreamUrl){

        def endpointNameAndStage = stage+'-'+endpointName

        steps.sh("echo $endpointNameAndStage")
        steps.sh("echo $endpointUpstreamUrl")
        steps.sh("echo $endpointPath")

        def kongServiceRequestBody = [
            name : endpointNameAndStage,
            url: endpointUpstreamUrl
        ]
        def kongServiceRequestBodyJSON = new JsonBuilder(kongServiceRequestBody).toString()

        def serviceId = retrieveServiceIdByName(endpointNameAndStage)
        serviceId =  createOrUpdateService(kongServiceRequestBodyJSON,endpointNameAndStage )


        if(serviceId != null){
            // Registering Route in KONG 
            def routePath = '/'+ stage + endpointPath
            def kongRoutesRequestBody = [
                service: [ id: serviceId ] ,
                protocols: ['https'],
                hosts: [kongHost],
                methods: ['GET'],
                paths: [routePath]
            ] 

            def kongRoutesRequestBodyJSON = new JsonBuilder(kongRoutesRequestBody).toString()
            def routeId = registerRoute(kongRoutesRequestBodyJSON)

        }

    }

    @NonCPS
    def sanitize(url) {
        def sanitized = url.replace ("//","/")
        sanitized = sanitized.toLowerCase()
        return sanitized
    }

    def retrieveServiceIdByName(serviceName){
        def serviceUrl = this.kongServicesUrl + serviceName
        def serviceId
        try{
            def serviceResponse = steps.httpRequest url: serviceUrl, httpMode: 'GET', contentType: 'APPLICATION_JSON', consoleLogResponseBody: true
            def serviceResponseJSON = new JsonSlurper().parseText(serviceResponse.content)
            serviceId = serviceResponseJSON.id
        }catch(e){
            // Service not found
            serviceId = null
        }
        return serviceId
    }


    def createOrUpdateService(kongServiceRequestBodyJSON, serviceName){
        def serviceUrl = this.kongServicesUrl + serviceName
        def serviceId
        try{
            steps.sh("echo \"Updating: $serviceUrl\"")
            def serviceResponse = steps.httpRequest url: serviceUrl, httpMode: 'PUT', contentType: 'APPLICATION_JSON', requestBody: kongServiceRequestBodyJSON, consoleLogResponseBody: true
            steps.sh("echo \"Updated: $serviceResponse\"")

            def serviceResponseJSON = new JsonSlurper().parseText(serviceResponse.content)
            serviceId = serviceResponseJSON.id
        }catch(e){
            // Error registering service
            serviceId = null
        }
        return serviceId
    }

    def registerRoute(kongRoutesRequestBodyJSON){
        def routeId
        def routeUrl = this.kongRoutesUrl 
        try{
            def routeResponse = steps.httpRequest url: routeUrl, httpMode: 'POST', contentType: 'APPLICATION_JSON', requestBody: kongRoutesRequestBodyJSON, consoleLogResponseBody: true
            def routeResponseJSON = new JsonSlurper().parseText(routeResponse.content)
            route = routeResponseJSON.id
        }catch(e){
            // Error registering route
            routeId = null
        }
        return routeId
    }

}
