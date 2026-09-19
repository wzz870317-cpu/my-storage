#include<iostream>
using namespace std;
int main()
{
	int num1,num2;
	cout<<"请输入一个整数";
	cin>>num1;
	cout<<"请输入另一个整数";
	cin>>num2;
	bool is_big = num1 > num2;
	bool is_small = num1 < num2;
	bool is_equal = num1 == num2;
	if(is_big)
	cout<<num1<<" 大于 "<<num2;
	if(is_small)
	cout<<num1<<" 小于 "<<num2;
	if(is_equal)
	cout<<num1<<" 等于 "<<num2;
	return 0;
}
