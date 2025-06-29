#include<bits/stdc++.h>
using namespace std;

void solve(){
    long long x,y;
    cin>>x>>y;
    if(x<y){
        long long ans;
        if(y%2)
            ans = (y*y)-x+1;
        else   
            ans = ((y-1)*(y-1))+x;
        cout<<ans<<endl;
    }
    else{
        long long ans;
        if(x%2)
            ans = ((x-1)*(x-1))+y;
        else    
            ans = (x*x)-y+1;
        cout<<ans<<endl;
    }
}

int main(){
    int t;
    cin>>t;
    while(t--){
        solve();
    }
    return 0;
}