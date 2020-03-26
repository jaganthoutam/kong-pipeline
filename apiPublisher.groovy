def kongManager = new com.fts.api.kong.KongManager(this, templateParams.kongProtocol, templateParams.kongHost, templateParams.kongAdminHost, templateParams.kongPort)

stage('PUBLISH KONG')  
  container(name:'apiary-kong', shell: '/bin/bash') {
    try{
      kongManager.register(true,  'ci', 'test-endpoint', '', '/test', 'http://my-test-url.com/test)
    }catch(error){
      print("This error is because kong is not accessible or the endpoint was not registered")
    }
  }
}
