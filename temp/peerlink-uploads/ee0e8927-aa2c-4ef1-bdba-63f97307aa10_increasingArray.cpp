#include<bits/stdc++.h>
using namespace std;

int main(){
    long long n;
    cin>>n;
    long long arr[n];
    for(long long i=0; i<n; i++)  cin>>arr[i];
    long long mini = arr[0];
    long long moves = 0;
    long long diff = 0;
    for(long long i=1; i<n; i++){
        diff = mini - arr[i];
        if(diff>0){
            moves += diff;
        }
        else if(arr[i] > mini){
            mini = arr[i];
        }
    }
    cout<<moves<<endl;
    return 0;
}