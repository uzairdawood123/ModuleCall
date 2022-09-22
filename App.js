/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React from 'react';
import {NativeModules} from 'react-native';
import { NativeEventEmitter } from 'react-native';
const { HelloWorldModule } = NativeModules;




import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  useColorScheme,
  View,
  requireNativeComponent
} from 'react-native';
var HelloWorld = NativeModules.HelloWorld;

// const Bulbss = requireNativeComponent("Bulb")


import { render } from "react-native/Libraries/Renderer/implementations/ReactFabric-prod";




class App extends React.Component {
  constructor() {
    super()
    this.state = {
      count: 0,
      isShown: false

    }



  }

  componentDidMount() {
    const eventEmitter = new NativeEventEmitter(NativeModules.ToastExample);
    this.eventListener = eventEmitter.addListener('SomeEventName', (event) => {
      console.log(event.eventProperty)
    })

      // const getDeviceAttributes =( =>
    // HelloWorld.sayHi( (err) => {console.log("errorr",err)}, (msg) => {console.log("thiss",msg)} );

    // DeviceEventEmitter.addListener('SomeEventName', function(e: Event) {
    //   console.log("this",e.eventProperty) // "someValue"
    // });

    // const eventEmitter = new NativeEventEmitter(NativeModules.ToastExample);
    // this.eventListener = eventEmitter.addListener('SomeEventName', (event) => {
    //   console.log(event.eventProperty) // "someValue"
    // });
  }

    async sayHiFromJava() {
    HelloWorldModule.NavigationNative()

      // HelloWorldModule.sayHi();
  }

  async makeCall() {
    // HelloWorldModule.NavigationNative()

    // HelloWorldModule.sayHi();
  }

  // async addNum() {
  //   HelloWorld.addTwo((msg)=> {console.log("result", msg)},4,4)
  //   // console.log("result",HelloWorld.addTwo( 2,3));
  // }

  // async multiplis() {
  //   HelloWorld.numMulti(1, 65, (msg)=> {console.log("result", msg)})
  //   // return console.log(HelloWorld.numMulti(1,2));
  // }
  increaseOne = () => {
    this.setState({count: this.state.count + 1})
  }
  deacreaseOne = () => {
    this.setState({count: this.state.count - 1})

  }
  render() {
    return (


      <View style={{flexDirection: "column",width: "100%" }}>
        <TouchableOpacity
          onPress={this.sayHiFromJava}>
          <Text>Public single screen share</Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={this.sayHiFromJava}>
          <Text>call screen</Text>
        </TouchableOpacity>
        {/*<View style={styles.top} />*/}
        {/*<Bulbss style={ styles.bottom } />*/}
      </View>
    );
  }
}
// const styles = StyleSheet.create({
//   container: {
//     flex: 1,
//     backgroundColor: '#F5FCFF',
//   },
//   top: {
//     flex: 1,
//     alignItems: "center",
//     justifyContent: "center",
//   },
//   bottom: {
//     flex: 1,
//     alignItems: "center",
//     justifyContent: "center",
//   },
// });






export default App;
