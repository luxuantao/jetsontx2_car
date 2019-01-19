int Echo = A1; //Echo回声脚(P2.0)
int Trig =A0;  //Trig触发脚(P2.1)

int Front_Distance = 0;
int Left_Distance = 0;
int Right_Distance = 0;

int Left_motor_go=8;     //左电机前进(IN1)
int Left_motor_back=9;   //左电机后退(IN2)
int Right_motor_go=10;   //右电机前进(IN3)
int Right_motor_back=11; //右电机后退(IN4)

int key=A2;  //定义按键A2接口
int beep=A3; //定义蜂鸣器A3接口

const int SensorRight_2 = 5; //前红外传感器(P3.5 OUT4)
const int HWR = 13; //右红外传感器
const int HWL = 6; //左红外传感器
int LED=7; //定义LED 数字7接口

int SR_2; //前红外传感器状态
int HR; //右红外传感器状态
int HL; //左红外传感器状态
int servopin=2; //设置舵机驱动脚到数字口2
int myangle; //定义角度变量
int pulsewidth; //定义脉宽变量
int val;

int state = 0;
char getstr;

void setup()
{
    Serial.begin(9600); //初始化串口
    //初始化电机驱动IO为输出方式
    pinMode(Left_motor_go,OUTPUT);   // PIN 8 (PWM)
    pinMode(Left_motor_back,OUTPUT); // PIN 9 (PWM)
    pinMode(Right_motor_go,OUTPUT);  // PIN 10 (PWM)
    pinMode(Right_motor_back,OUTPUT);// PIN 11 (PWM)
    pinMode(HWR,INPUT);
    pinMode(key,INPUT); //定义按键接口为输入接口
    pinMode(beep,OUTPUT);
    pinMode(LED,OUTPUT); //定义LED为输出接口
    pinMode(SensorRight_2, INPUT); //定义右红外传感器为输入
    //初始化超声波引脚
    pinMode(Echo, INPUT);    // 定义超声波输入脚
    pinMode(Trig, OUTPUT);   // 定义超声波输出脚
    pinMode(servopin,OUTPUT);//设定舵机接口为输出接口
}

//=======================小车的基本动作=========================
//void run(int time) //前进
void run() //前进
{
    digitalWrite(Right_motor_go,HIGH); //右电机前进
    digitalWrite(Right_motor_back,LOW);
    analogWrite(Right_motor_go,131); //PWM比例0~255调速，左右轮差异略增减
    analogWrite(Right_motor_back,0);
    digitalWrite(Left_motor_go,LOW); //左电机前进
    digitalWrite(Left_motor_back,HIGH);
    analogWrite(Left_motor_go,0);
    analogWrite(Left_motor_back,145); 
    //delay(time * 100); //执行时间，可以调整
}

void brake(int time) //刹车，停车
{
    digitalWrite(Right_motor_go,LOW);
    digitalWrite(Right_motor_back,LOW);
    digitalWrite(Left_motor_go,LOW);
    digitalWrite(Left_motor_back,LOW);
    delay(time * 100); //执行时间，可以调整
}

void left(int time) //左转(左轮不动，右轮前进)
{
    digitalWrite(Right_motor_go,HIGH); //右电机前进
    digitalWrite(Right_motor_back,LOW);
    analogWrite(Right_motor_go,180);
    analogWrite(Right_motor_back,0); 
    digitalWrite(Left_motor_go,LOW); //左轮不动
    digitalWrite(Left_motor_back,LOW);
    analogWrite(Left_motor_go,0);
    analogWrite(Left_motor_back,0); 
    delay(time * 100); //执行时间，可以调整
}

void spin_left(int time) //左转(左轮后退，右轮前进)
{
    digitalWrite(Right_motor_go,HIGH); //右电机前进
    digitalWrite(Right_motor_back,LOW);
    analogWrite(Right_motor_go,200);
    analogWrite(Right_motor_back,0); 
    digitalWrite(Left_motor_go,HIGH); //左轮后退
    digitalWrite(Left_motor_back,LOW);
    analogWrite(Left_motor_go,200);
    analogWrite(Left_motor_back,0); 
    delay(time * 100); //执行时间，可以调整
}

void right(int time) //右转(右轮不动，左轮前进)
{
    digitalWrite(Right_motor_go,LOW); //右电机后退
    digitalWrite(Right_motor_back,LOW);
    analogWrite(Right_motor_go,0);
    analogWrite(Right_motor_back,0);
    digitalWrite(Left_motor_go,LOW); //左电机前进
    digitalWrite(Left_motor_back,HIGH);
    analogWrite(Left_motor_go,0);
    analogWrite(Left_motor_back,200);
    delay(time * 100); //执行时间，可以调整
}

void spin_right(int time) //右转(右轮后退，左轮前进)
{
    digitalWrite(Right_motor_go,LOW); //右电机后退
    digitalWrite(Right_motor_back,HIGH);
    analogWrite(Right_motor_go,0);
    analogWrite(Right_motor_back,150); 
    digitalWrite(Left_motor_go,LOW); //左电机前进
    digitalWrite(Left_motor_back,HIGH);
    analogWrite(Left_motor_go,0);
    analogWrite(Left_motor_back,150);
    delay(time * 100);  //执行时间，可以调整
}

void back(int time) //后退
{
    digitalWrite(Right_motor_go,LOW); //右轮后退
    digitalWrite(Right_motor_back,HIGH);
    analogWrite(Right_motor_go,0);
    analogWrite(Right_motor_back,150);
    digitalWrite(Left_motor_go,HIGH); //左轮后退
    digitalWrite(Left_motor_back,LOW);
    analogWrite(Left_motor_go,150);
    analogWrite(Left_motor_back,0);
    delay(time * 100); //执行时间，可以调整
}

//==========================================================

void keyscan() //按键扫描
{
    int val;
    val=digitalRead(key); //读取数字7口电平值赋给val
    while(!digitalRead(key)) //当按键没被按下时，一直循环
    {
        val=digitalRead(key); //此句可省略，可让循环跑空
    }
    while(digitalRead(key))//当按键被按下时
    {
        delay(10); //延时10ms
        val=digitalRead(key); //读取数字7 口电平值赋给val
        if(val==HIGH) //第二次判断按键是否被按下
        {
            digitalWrite(beep,HIGH); //蜂鸣器响
            while(!digitalRead(key)) //判断按键是否被松开
            {
                digitalWrite(beep,LOW); //蜂鸣器停止
            }
        }
        else
            digitalWrite(beep,LOW); //蜂鸣器停止
    }
}

float Distance_test() //量出前方距离
{
    digitalWrite(Trig, LOW); //给触发脚低电平2μs
    delayMicroseconds(2);
    digitalWrite(Trig, HIGH); //给触发脚高电平10μs，这里至少是10μs
    delayMicroseconds(10);
    digitalWrite(Trig, LOW);  //持续给触发脚低电
    float Fdistance = pulseIn(Echo, HIGH); //读取高电平时间(单位：微秒)
    Fdistance = Fdistance/58; //为什么除以58等于厘米，Y米=（X秒*344）/2
    //X秒=（ 2*Y米）/344 ==》X秒=0.0058*Y米 ==》厘米=微秒/58
    //Serial.print("Distance:"); //输出距离（单位：厘米）
    //Serial.println(Fdistance); //显示距离
    //Distance = Fdistance;
    return Fdistance;
}

void servopulse(int servopin,int myangle) //定义一个脉冲函数，用来模拟方式产生PWM值舵机的范围是0.5MS到2.5MS 1.5MS 占空比是居中周期是20MS
{
    pulsewidth=(myangle*11)+500; //将角度转化为500-2480 的脉宽值 这里的myangle就是0-180度  所以180*11+50=2480  11是为了换成90度的时候基本就是1.5MS
    digitalWrite(servopin,HIGH); //将舵机接口电平置高                                      90*11+50=1490uS  就是1.5ms
    delayMicroseconds(pulsewidth); //延时脉宽值的微秒数  这里调用的是微秒延时函数
    digitalWrite(servopin,LOW); //将舵机接口电平置低
    delay(20-(pulsewidth*0.001)); //延时周期内剩余时间  这里调用的是ms延时函数
}

void front_detection()
{
    //此处循环次数减少，为了增加小车遇到障碍物的反应速度
    for(int i=0; i<=5; i++) //产生PWM个数，等效延时以保证能转到响应角度
    {
        servopulse(servopin,90);//模拟产生PWM
    }
    Front_Distance = Distance_test();
    //Serial.print("Front_Distance:"); //输出距离（单位：厘米）
    //Serial.println(Front_Distance);  //显示距离
    //Distance_display(Front_Distance);
}

void left_detection()
{
    for(int i=0; i<=15; i++) //产生PWM个数，等效延时以保证能转到响应角度
    {
        servopulse(servopin,175); //模拟产生PWM
    }
    Left_Distance = Distance_test();
    //Serial.print("Left_Distance:"); //输出距离（单位：厘米）
    //Serial.println(Left_Distance);  //显示距离
}

void right_detection()
{
    for(int i=0; i<=15; i++) //产生PWM个数，等效延时以保证能转到响应角度
    {
        servopulse(servopin,5); //模拟产生PWM
    }
    Right_Distance = Distance_test();
    //Serial.print("Right_Distance:"); //输出距离（单位：厘米）
    //Serial.println(Right_Distance); //显示距离
}

//===========================================================

void loop()
{
    //================手动部分============
    if (state == 0)
    {
        getstr=Serial.read(); 
        if(getstr=='A')
        {
            Serial.println("go forward!");
            run();
        }
        else if(getstr=='B')
        {
            Serial.println("go back!");
            back(1);
        }
        else if(getstr=='C')
        {
            Serial.println("go left!");
            left(1);
        }
        else if(getstr=='D')
        {
            Serial.println("go right!");
            right(1);
        }
        else if(getstr=='F')
        {
            Serial.println("Stop!");
            brake(1);
        }
        else if(getstr=='E')
        {
            Serial.println("Stop!");
            brake(1);
        }
        else if(getstr=='Z')
        {
            Serial.println("Switch!");
            state=1;
         }
    }
    //================自动部分=============
    else
    {
        //    keyscan(); //调用按键扫描函数
        while(1)
        {
            getstr=Serial.read(); 
            if(getstr=='Z')
            {
                state=0;
                brake(1);
                break;
             }
            //红外部分：有信号为LOW  没有信号为HIGH  有障碍物输出0  没有障碍物输出1
            //正前方
            SR_2 = digitalRead(SensorRight_2);
            if (SR_2==HIGH) //前面没有障碍物
            {
                run(); //调用前进函数
                digitalWrite(beep,LOW); //蜂鸣器不响
                digitalWrite(LED,LOW);  //LED不亮
            }
            else if (SR_2 == LOW) //前面探测到有障碍物，有信号返回
            {
                digitalWrite(beep,HIGH); //蜂鸣器响
                digitalWrite(LED,HIGH);  //LED亮
                brake(3);//停止300MS
                back(8);//后退800MS
                left(5);//调用左转函数，延时500ms
            }
            //右侧
            HR = digitalRead(HWR);
            if (HR==HIGH) //前面没有障碍物
            {
                run(); //调用前进函数
                digitalWrite(beep,LOW); //蜂鸣器不响
                digitalWrite(LED,LOW);  //LED不亮
            }
            else if ( HR == LOW) //前面探测到有障碍物，有信号返回
            {
                digitalWrite(beep,HIGH); //蜂鸣器响
                digitalWrite(LED,HIGH);  //LED亮
                //brake(0); //停止
                //back(0); //后退
                left(2); //调用左转函数，延时
            }
            //左侧
            HL = digitalRead(HWL);
            if (HL==HIGH) //前面没有障碍物
            {
                run(); //调用前进函数
                digitalWrite(beep,LOW); //蜂鸣器不响
                digitalWrite(LED,LOW);  //LED不亮
            }
            else if ( HL == LOW) //前面探测到有障碍物，有信号返回
            {
                digitalWrite(beep,HIGH); //蜂鸣器响
                digitalWrite(LED,HIGH);  //LED亮
                //brake(0);//停止
                //back(0);//后退
                right(2);//调用左转函数，延时
            }
    
            //超声波部分
            front_detection(); //测量前方距离
            if(Front_Distance < 20) //当遇到障碍物时
            {
                brake(2);//先刹车
                back(3);//后退减速
                brake(2);//停下来做测距
                left_detection();//测量左边距障碍物距离
                right_detection();//测量右边距障碍物距离
                if((Left_Distance < 20) && (Right_Distance < 20)) //当左右两侧均有障碍物靠得比较近
                {
                    spin_left(0.7);//旋转掉头
                }
                else if(Left_Distance > Right_Distance) //左边比右边空旷
                {
                    left(6);//左转
                    brake(1);//刹车，稳定方向
                }
                else //右边比左边空旷
                {
                    right(6);//右转
                    brake(1);//刹车，稳定方向
                }
            }
            else
            {
                run(); //无障碍物，直行
            }
        }
    }
}
