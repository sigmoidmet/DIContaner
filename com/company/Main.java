package com.company;

public class Main {

    public void test(int a) {
        System.out.println(a);
    }

    public static void main(String[] args) throws Exception {
        DIContainer dContainer = new DIContainer(HardBeansConfig.class);
        StarSystem sSys = dContainer.getBean("Ssys");
        StarSystem sSys2 = dContainer.getBean("Ssys2");
        StarSystem sSys3 = dContainer.getBean("Ssys3");
        System.out.println(sSys.toString());
        System.out.println(sSys2.toString());
        System.out.println(sSys3.toString());

    }
}
